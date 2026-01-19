package com.cowtrack.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GeofenceResponse {
    private Long geofenceId;
    private Long cowId;
    private String cowName;
    private BigDecimal centerLatitude;
    private BigDecimal centerLongitude;
    private Integer radiusMeters;
    private LocalDateTime createdAt;
    private Boolean isActive = true;
}