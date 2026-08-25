package com.cowtrack.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetricResponse {
    private Long metricId;
    private Long cowId;
    private String cowName;
    private BigDecimal temperature;
    private BigDecimal heartRate;
    private BigDecimal activityLevel;
    private LocalDateTime recordedAt;
    /** "Normal" or "Warning", from the thresholds on HealthMetric. */
    private String status;
}
