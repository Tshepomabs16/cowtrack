package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "geofences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Geofence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "geofence_id")
    private Long geofenceId;

    @OneToOne
    @JoinColumn(name = "cow_id", nullable = false, unique = true)
    private Cow cow;

    @Column(name = "center_latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal centerLatitude;

    @Column(name = "center_longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal centerLongitude;

    @Column(name = "radius_meters", nullable = false)
    private Integer radiusMeters;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}