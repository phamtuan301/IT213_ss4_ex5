package com.example.logistics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Thực thể IncidentReport đại diện cho thông tin sự cố được lưu vào cơ sở dữ liệu.
 */
@Entity
@Table(name = "incident_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false)
    private String orderCode;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Column(name = "incident_type", nullable = false)
    private String incidentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false)
    private Urgency urgency;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "incident_time", nullable = false)
    private LocalDateTime incidentTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false)
    private NotificationStatus notificationStatus;
}
