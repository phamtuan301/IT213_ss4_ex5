package com.example.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) chứa kết quả bóc tách thông tin từ AI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentExtraction {
    private String orderCode;
    private String licensePlate;
    private String incidentType;
    private String urgency; // Sẽ map sang Urgency enum sau khi validate
    private String description;
    private String incidentTime; // Có thể có định dạng chuỗi ISO hoặc rỗng
}
