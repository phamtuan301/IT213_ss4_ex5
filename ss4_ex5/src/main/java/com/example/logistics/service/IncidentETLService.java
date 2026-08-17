package com.example.logistics.service;

import com.example.logistics.dto.IncidentExtraction;
import com.example.logistics.model.IncidentReport;
import com.example.logistics.model.NotificationStatus;
import com.example.logistics.model.Urgency;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service điều phối quy trình ETL sự cố:
 * 1. Nhận tin nhắn thô từ tài xế.
 * 2. Giả lập/Gọi AI bóc tách thông tin (JSON).
 * 3. Làm sạch phản hồi từ AI (Clean Markdown).
 * 4. Parse JSON và thực hiện defensive validation.
 * 5. Thực hiện Phase 1: Lưu thông tin sự cố với trạng thái ban đầu (PENDING hoặc NOT_REQUIRED).
 * 6. Kiểm tra mức độ khẩn cấp (Urgency).
 * 7. Thực hiện Phase 2: Gửi cảnh báo khẩn cấp và cập nhật trạng thái tương ứng (SUCCESS hoặc FAILED).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentETLService {

    private final IncidentPersistenceService persistenceService;
    private final ConsoleAlertService consoleAlertService;
    private final ObjectMapper objectMapper;

    /**
     * Quy trình xử lý ETL sự cố từ tin nhắn thô.
     *
     * @param rawMessage Tin nhắn thô từ tài xế hoặc giả lập phản hồi JSON từ AI.
     * @return IncidentReport đã xử lý và lưu trữ.
     */
    public IncidentReport processRawIncident(String rawMessage) {
        log.info("Starting incident ETL processing");

        try {
            // Bước 1: Clean AI response (Markdown cleanup)
            String cleanedJson = cleanAIResponse(rawMessage);

            // Bước 2: Parse JSON
            IncidentExtraction extraction = parseJson(cleanedJson);

            // Bước 3: Defensive validation
            validateExtraction(extraction);
            log.info("AI extraction completed and validated successfully");

            // Bước 4: Map DTO to Entity
            IncidentReport report = mapToEntity(extraction);

            // Xác định trạng thái thông báo ban đầu dựa trên độ khẩn cấp (Urgency)
            NotificationStatus initialStatus = determineInitialNotificationStatus(report.getUrgency());
            report.setNotificationStatus(initialStatus);

            // PHASE 1: Lưu thông tin sự cố ban đầu vào Database (Commit Phase 1)
            IncidentReport savedReport = persistenceService.saveIncident(report);

            // PHASE 2: Cảnh báo khẩn cấp nếu mức độ là HIGH hoặc CRITICAL
            if (isEmergencyUrgency(savedReport.getUrgency())) {
                log.info("Incident urgency={}, emergency notification required", savedReport.getUrgency());
                try {
                    // Gọi dịch vụ cảnh báo console (External service call)
                    consoleAlertService.sendAlert(savedReport);

                    // Cập nhật trạng thái SUCCESS trong transaction mới
                    persistenceService.updateNotificationStatus(savedReport.getId(), NotificationStatus.SUCCESS);
                    savedReport.setNotificationStatus(NotificationStatus.SUCCESS);

                } catch (Exception ex) {
                    log.error("Emergency alert failed for incident id={}", savedReport.getId(), ex);

                    // Cập nhật trạng thái FAILED trong transaction mới (không ảnh hưởng tới Phase 1 đã commit)
                    persistenceService.updateNotificationStatus(savedReport.getId(), NotificationStatus.FAILED);
                    savedReport.setNotificationStatus(NotificationStatus.FAILED);
                }
            } else {
                log.info("Incident urgency={}, no alert required. Notification status is NOT_REQUIRED", savedReport.getUrgency());
            }

            return savedReport;

        } catch (Exception e) {
            log.error("Failed to process raw incident message due to error: {}", e.getMessage(), e);
            throw new RuntimeException("Incident processing failed", e);
        }
    }

    /**
     * Làm sạch markdown code block trong chuỗi JSON trả về từ AI.
     */
    private String cleanAIResponse(String rawResponse) {
        if (rawResponse == null) {
            return "";
        }
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    /**
     * Parse chuỗi JSON sang DTO.
     */
    private IncidentExtraction parseJson(String json) throws Exception {
        return objectMapper.readValue(json, IncidentExtraction.class);
    }

    /**
     * Thực hiện Defensive Validation kiểm tra dữ liệu đầu vào từ AI.
     */
    private void validateExtraction(IncidentExtraction extraction) {
        if (extraction.getOrderCode() == null || extraction.getOrderCode().isBlank()) {
            throw new IllegalArgumentException("Validation failed: orderCode is empty or missing.");
        }
        if (extraction.getLicensePlate() == null || extraction.getLicensePlate().isBlank()) {
            throw new IllegalArgumentException("Validation failed: licensePlate is empty or missing.");
        }
        if (extraction.getIncidentType() == null || extraction.getIncidentType().isBlank()) {
            throw new IllegalArgumentException("Validation failed: incidentType is empty or missing.");
        }
        if (extraction.getUrgency() == null || extraction.getUrgency().isBlank()) {
            throw new IllegalArgumentException("Validation failed: urgency level is empty or missing.");
        }
        try {
            Urgency.valueOf(extraction.getUrgency().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Validation failed: Invalid urgency value: " + extraction.getUrgency());
        }
    }

    /**
     * Ánh xạ thông tin từ DTO sang JPA Entity.
     */
    private IncidentReport mapToEntity(IncidentExtraction extraction) {
        Urgency urgency = Urgency.valueOf(extraction.getUrgency().toUpperCase());
        LocalDateTime time = (extraction.getIncidentTime() != null && !extraction.getIncidentTime().isBlank())
                ? LocalDateTime.parse(extraction.getIncidentTime())
                : LocalDateTime.now();

        return IncidentReport.builder()
                .orderCode(extraction.getOrderCode())
                .licensePlate(extraction.getLicensePlate())
                .incidentType(extraction.getIncidentType())
                .urgency(urgency)
                .description(extraction.getDescription())
                .incidentTime(time)
                .build();
    }

    /**
     * Xác định trạng thái thông báo ban đầu.
     */
    private NotificationStatus determineInitialNotificationStatus(Urgency urgency) {
        return switch (urgency) {
            case LOW, MEDIUM -> NotificationStatus.NOT_REQUIRED;
            case HIGH, CRITICAL -> NotificationStatus.PENDING;
        };
    }

    /**
     * Kiểm tra xem mức độ khẩn cấp có cần kích hoạt thông báo đỏ hay không.
     */
    private boolean isEmergencyUrgency(Urgency urgency) {
        return switch (urgency) {
            case HIGH, CRITICAL -> true;
            default -> false;
        };
    }
}
