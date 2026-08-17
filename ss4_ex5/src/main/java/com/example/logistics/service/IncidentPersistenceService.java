package com.example.logistics.service;

import com.example.logistics.model.IncidentReport;
import com.example.logistics.model.NotificationStatus;
import com.example.logistics.repository.IncidentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service quản lý ranh giới giao dịch (Transaction Boundary) cho thực thể IncidentReport.
 * Phân tách Phase 1 (Save) và Phase 2 (Update Status) thành các giao dịch riêng biệt
 * để tránh việc rollback lan truyền và lỗi self-invocation trong Spring AOP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentPersistenceService {

    private final IncidentRepository incidentRepository;

    /**
     * PHASE 1: Lưu thông tin sự cố ban đầu vào Cơ sở dữ liệu.
     * Phương thức này thực thi trong một Transaction riêng biệt và commit ngay khi hoàn thành.
     *
     * @param report Thực thể IncidentReport cần lưu.
     * @return IncidentReport sau khi được lưu (có kèm ID).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public IncidentReport saveIncident(IncidentReport report) {
        log.info("Saving incident report to DB: orderCode={}, urgency={}", report.getOrderCode(), report.getUrgency());
        IncidentReport savedReport = incidentRepository.save(report);
        log.info("Incident saved successfully: id={}, status={}", savedReport.getId(), savedReport.getNotificationStatus());
        return savedReport;
    }

    /**
     * PHASE 2: Cập nhật trạng thái thông báo sự cố.
     * Sử dụng Propagation.REQUIRES_NEW để đảm bảo cập nhật trạng thái chạy trong một Transaction mới hoàn toàn,
     * độc lập với Transaction của Phase 1 hoặc các ngoại lệ từ dịch vụ bên ngoài (ConsoleAlertService).
     *
     * @param id     ID của IncidentReport.
     * @param status Trạng thái mới cần cập nhật.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateNotificationStatus(Long id, NotificationStatus status) {
        log.info("Updating incident ID={} notification status to: {}", id, status);
        IncidentReport report = incidentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("IncidentReport not found with ID: " + id));
        report.setNotificationStatus(status);
        incidentRepository.save(report);
        log.info("Notification status updated successfully to: {} for incident ID={}", status, id);
    }
}
