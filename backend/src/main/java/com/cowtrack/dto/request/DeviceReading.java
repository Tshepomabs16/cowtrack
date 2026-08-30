package com.cowtrack.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One sample from a collar.
 *
 * <p>Position and vitals are both optional and independent: hardware varies, and
 * a collar that has a GPS fix but no heart-rate contact should still report the
 * position. A reading carrying neither is rejected as empty.
 *
 * <p>The timestamp comes from the device, not the server. A collar buffers while
 * out of coverage, so arrival time says nothing about when the animal was
 * actually there.
 */
@Data
public class DeviceReading {

    @NotNull(message = "Each reading must carry the time it was taken")
    private LocalDateTime recordedAt;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracy;

    private BigDecimal temperature;
    private BigDecimal heartRate;
    private BigDecimal activityLevel;

    public boolean hasPosition() {
        return latitude != null && longitude != null;
    }

    public boolean hasVitals() {
        return temperature != null || heartRate != null || activityLevel != null;
    }
}
