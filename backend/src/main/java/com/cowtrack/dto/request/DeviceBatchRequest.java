package com.cowtrack.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * An upload from one collar: what the device knows about itself, plus whatever
 * it has buffered since it was last able to reach us.
 */
@Data
public class DeviceBatchRequest {

    /** Charge at the time of upload, 0-100. */
    @Min(value = 0, message = "Battery percentage cannot be negative")
    @Max(value = 100, message = "Battery percentage cannot exceed 100")
    private Integer batteryPercent;

    private String firmwareVersion;

    /**
     * Capped so one upload cannot exhaust memory or hold a transaction open
     * indefinitely. A collar with more buffered than this sends several batches.
     */
    @NotEmpty(message = "A batch must contain at least one reading")
    @Size(max = 500, message = "A batch may contain at most 500 readings")
    @Valid
    private List<DeviceReading> readings;
}
