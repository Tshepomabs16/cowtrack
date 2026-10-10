package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A piece of the farm drawn on the map.
 *
 * <p>A {@link FenceType#KEEP_IN} fence is a camp: animals are assigned to it
 * ({@link Cow#getCamp()}) and leaving it raises an alert. A
 * {@link FenceType#KEEP_OUT} fence is a restricted zone - a dam, a poisonous
 * plant patch, a neighbour's crop - that applies to the whole herd.
 */
@Entity
@Table(name = "geofences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Geofence {

    public enum FenceType {
        KEEP_IN,
        KEEP_OUT
    }

    public enum Shape {
        CIRCLE,
        POLYGON
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "geofence_id")
    private Long geofenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "fence_type", nullable = false, length = 16)
    private FenceType fenceType = FenceType.KEEP_IN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Shape shape = Shape.CIRCLE;

    @Column(name = "center_latitude", precision = 9, scale = 6)
    private BigDecimal centerLatitude;

    @Column(name = "center_longitude", precision = 9, scale = 6)
    private BigDecimal centerLongitude;

    @Column(name = "radius_meters")
    private Integer radiusMeters;

    /** Corners as {@code [[lat,lng],...]} for a polygon; null for a circle. */
    @Column(name = "vertices_json", length = 20000)
    private String verticesJson;

    /** Off while a camp is rested in a grazing rotation; nothing is evaluated against it. */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Set instead of deleting once the fence has raised alerts, so they keep their context. */
    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isKeepIn() {
        return fenceType == FenceType.KEEP_IN;
    }
}
