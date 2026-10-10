package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Which side of one fence one animal was last confirmed on.
 *
 * <p>Kept so an alert is raised when that changes, rather than on every
 * position, and so the decision does not depend on which two fixes happen to be
 * newest. See {@link com.cowtrack.util.FenceCrossing} for the rule.
 */
@Entity
@Table(name = "geofence_occupancy")
@IdClass(GeofenceOccupancy.Key.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceOccupancy {

    @Id
    @Column(name = "geofence_id")
    private Long geofenceId;

    @Id
    @Column(name = "cow_id")
    private Long cowId;

    @Column(name = "is_inside", nullable = false)
    private Boolean isInside;

    /** Fixes in a row that have landed on the other side without yet confirming it. */
    @Column(name = "pending_fixes", nullable = false)
    private Integer pendingFixes = 0;

    /** When the fix this state was last updated from was taken. Older fixes are not evaluated. */
    @Column(name = "last_fix_at", nullable = false)
    private LocalDateTime lastFixAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private Long geofenceId;
        private Long cowId;
    }
}
