package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne
    @JoinColumn(name = "cow_id", nullable = false)
    private Cow cow;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    private String message;

    @Column(name = "is_resolved")
    private Boolean isResolved = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** The fence a breach was raised against; null for every other alert type. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geofence_id")
    private Geofence geofence;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** Why it was closed, when the system closed it, e.g. "Returned inside 'North camp'". */
    @Column(name = "resolution_note")
    private String resolutionNote;

    public enum AlertType {
        GEOFENCE_BREACH,
        NO_SIGNAL,
        NIGHT_MOVEMENT,
        DEVICE_REMOVED,
        LOW_BATTERY
    }
}