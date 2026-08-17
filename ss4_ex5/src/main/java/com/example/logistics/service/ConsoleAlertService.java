package com.example.logistics.service;

import com.example.logistics.model.IncidentReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service đảm nhận việc gửi cảnh báo khẩn cấp lên console sử dụng SLF4J.
 * Có hỗ trợ giả lập lỗi dựa trên thuộc tính cấu hình.
 */
@Slf4j
@Service
public class ConsoleAlertService {

    @Value("${incident.alert.simulate-failure:false}")
    private boolean simulateFailure;

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    /**
     * Gửi cảnh báo khẩn cấp ra Console.
     *
     * @param report Thông tin sự cố khẩn cấp.
     * @throws RuntimeException Nếu cấu hình simulateFailure được kích hoạt.
     */
    public void sendAlert(IncidentReport report) {
        if (simulateFailure) {
            log.warn("[SIMULATION] Simulating Console Alert failure for incident ID: {}", report.getId());
            throw new RuntimeException("Simulated console alert delivery exception!");
        }

        String alertMessage = String.format(
                "\n============================================================\n" +
                "                 !!! RED ALERT !!!\n" +
                "============================================================\n" +
                " INCIDENT KHẨN CẤP\n" +
                " Order Code   : %s\n" +
                " License Plate: %s\n" +
                " Incident Type: %s\n" +
                " Urgency      : %s\n" +
                " Description  : %s\n" +
                " Time         : %s\n" +
                "============================================================\n",
                report.getOrderCode(),
                report.getLicensePlate(),
                report.getIncidentType(),
                report.getUrgency(),
                report.getDescription(),
                report.getIncidentTime()
        );

        log.info(alertMessage);
        log.info("Emergency console alert sent successfully for incident ID: {}", report.getId());
    }
}
