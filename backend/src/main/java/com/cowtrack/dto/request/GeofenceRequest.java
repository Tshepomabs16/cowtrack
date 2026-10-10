package com.cowtrack.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * A camp or restricted zone as drawn on the map.
 *
 * <p>Field-level checks live here; the rules that span fields (a circle needs a
 * centre, a polygon needs corners that do not cross) are checked in the service,
 * because they depend on which shape was chosen.
 */
@Data
public class GeofenceRequest {

    @NotBlank(message = "A name is required")
    @Size(max = 120, message = "Name must be 120 characters or fewer")
    private String name;

    @Size(max = 500, message = "Description must be 500 characters or fewer")
    private String description;

    /** KEEP_IN (a camp, the default) or KEEP_OUT (a restricted zone). */
    private String fenceType;

    /** CIRCLE or POLYGON. */
    private String shape;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal centerLatitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal centerLongitude;

    @Positive(message = "Radius must be positive")
    private Integer radiusMeters;

    /** Polygon corners as {@code [[lat, lng], ...]}, without repeating the first. */
    @Size(max = 500, message = "A polygon may have at most 500 corners")
    private List<List<BigDecimal>> vertices;

    /** Defaults to true. */
    private Boolean isActive;

    /** Animals to put in the camp as it is created. Ignored for a restricted zone. */
    private List<Long> cowIds;
}
