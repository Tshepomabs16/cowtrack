package com.cowtrack.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HealthMetricRequest {

    @NotNull(message = "Cow ID is required")
    private Long cowId;

    private BigDecimal temperature;
    private BigDecimal heartRate;
    private BigDecimal activityLevel;

    /** Defaults to now when omitted. */
    private LocalDateTime recordedAt;
}
