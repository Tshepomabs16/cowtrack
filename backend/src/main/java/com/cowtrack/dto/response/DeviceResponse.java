package com.cowtrack.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceResponse {
    private Long deviceId;
    private String serialNumber;
    private Long cowId;
    private String cowName;
    private String status;
    private Integer batteryPercent;
    private boolean batteryLow;
    private String firmwareVersion;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;

    /**
     * The device's key, in the clear. Populated only by registration and
     * re-keying, never by a read: only the hash is stored, so this is the one
     * opportunity to copy it.
     */
    private String apiKey;
}
