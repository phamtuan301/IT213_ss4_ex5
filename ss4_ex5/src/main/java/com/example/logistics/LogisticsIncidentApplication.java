package com.example.logistics;

import com.example.logistics.model.IncidentReport;
import com.example.logistics.repository.IncidentRepository;
import com.example.logistics.service.ConsoleAlertService;
import com.example.logistics.service.IncidentETLService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@Slf4j
@SpringBootApplication
public class LogisticsIncidentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsIncidentApplication.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public CommandLineRunner demoRunner(
            IncidentETLService etlService,
            IncidentRepository repository,
            ConsoleAlertService consoleAlertService) {
        return args -> {
            log.info("\n=== BẮT ĐẦU CHẠY DEMO HỆ THỐNG AI LOGISTICS INCIDENT REPORTER ===");

            // -------------------------------------------------------------
            // CASE 1: Sự cố mức độ thấp (LOW) - Không cần gửi thông báo
            // -------------------------------------------------------------
            log.info("\n--- CASE 1: SỰ CỐ MỨC ĐỘ THẤP (LOW) ---");
            String rawMessageCase1 = """
                    ```json
                    {
                      "orderCode": "ORD-003",
                      "licensePlate": "29C-777.88",
                      "incidentType": "LATE_DELIVERY",
                      "urgency": "LOW",
                      "description": "Xe bị tắc đường nhẹ, giao hàng muộn khoảng 15 phút"
                    }
                    ```
                    """;
            try {
                IncidentReport report1 = etlService.processRawIncident(rawMessageCase1);
                log.info("CASE 1 HOÀN THÀNH. ID={}, Urgency={}, NotificationStatus={}",
                        report1.getId(), report1.getUrgency(), report1.getNotificationStatus());
            } catch (Exception e) {
                log.error("Lỗi Case 1: ", e);
            }

            // -------------------------------------------------------------
            // CASE 2: Sự cố khẩn cấp (CRITICAL) - Gửi thông báo THÀNH CÔNG
            // -------------------------------------------------------------
            log.info("\n--- CASE 2: SỰ CỐ KHẨN CẤP (CRITICAL) - THÀNH CÔNG ---");
            consoleAlertService.setSimulateFailure(false); // Đảm bảo không lỗi
            String rawMessageCase2 = """
                    ```json
                    {
                      "orderCode": "ORD-001",
                      "licensePlate": "29A-123.45",
                      "incidentType": "ACCIDENT",
                      "urgency": "CRITICAL",
                      "description": "Xe gặp sự cố tai nạn nghiêm trọng trên quốc lộ 1A"
                    }
                    ```
                    """;
            try {
                IncidentReport report2 = etlService.processRawIncident(rawMessageCase2);
                log.info("CASE 2 HOÀN THÀNH. ID={}, Urgency={}, NotificationStatus={}",
                        report2.getId(), report2.getUrgency(), report2.getNotificationStatus());
            } catch (Exception e) {
                log.error("Lỗi Case 2: ", e);
            }

            // -------------------------------------------------------------
            // CASE 3: Sự cố khẩn cấp (CRITICAL) - Gửi thông báo THẤT BẠI (FAULT TOLERANCE DEMO)
            // -------------------------------------------------------------
            log.info("\n--- CASE 3: SỰ CỐ KHẨN CẤP (CRITICAL) - THẤT BẠI (FAULT TOLERANCE) ---");
            consoleAlertService.setSimulateFailure(true); // Bật giả lập lỗi cảnh báo
            String rawMessageCase3 = """
                    ```json
                    {
                      "orderCode": "ORD-002",
                      "licensePlate": "30F-456.78",
                      "incidentType": "ACCIDENT",
                      "urgency": "CRITICAL",
                      "description": "Xe bị hỏng động cơ bốc khói dữ dội trên đường cao tốc"
                    }
                    ```
                    """;
            try {
                IncidentReport report3 = etlService.processRawIncident(rawMessageCase3);
                log.info("CASE 3 HOÀN THÀNH. ID={}, Urgency={}, NotificationStatus={}",
                        report3.getId(), report3.getUrgency(), report3.getNotificationStatus());
            } catch (Exception e) {
                log.error("Lỗi Case 3: ", e);
            }

            // -------------------------------------------------------------
            // IN KẾT QUẢ DATABASE CUỐI CÙNG ĐỂ KIỂM TRA
            // -------------------------------------------------------------
            log.info("\n=============================================================");
            log.info("            KẾT QUẢ TRONG DATABASE CUỐI CÙNG");
            log.info("=============================================================");
            List<IncidentReport> allReports = repository.findAll();
            log.info("Tổng số sự cố được lưu: {}", allReports.size());
            log.info(String.format("| %-4s | %-10s | %-13s | %-10s | %-20s |", "ID", "Order Code", "License Plate", "Urgency", "Notification Status"));
            log.info("-------------------------------------------------------------");
            for (IncidentReport r : allReports) {
                log.info(String.format("| %-4d | %-10s | %-13s | %-10s | %-20s |",
                        r.getId(), r.getOrderCode(), r.getLicensePlate(), r.getUrgency(), r.getNotificationStatus()));
            }
            log.info("=============================================================");

            log.info("\n=== HOÀN THÀNH CHẠY DEMO ===");
        };
    }
}
