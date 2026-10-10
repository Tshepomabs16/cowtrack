package com.cowtrack.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GeofenceResponse {
    private Long geofenceId;
    private String name;
    private String description;
    private String fenceType;
    private String shape;
    private BigDecimal centerLatitude;
    private BigDecimal centerLongitude;
    private Integer radiusMeters;
    private List<List<BigDecimal>> vertices;
    private Boolean isActive;
    private LocalDateTime retiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Animals assigned to this camp; always 0 for a restricted zone. */
    private long animalCount;

    /** Of those, how many were last confirmed outside it. */
    private long animalsOutside;
}
