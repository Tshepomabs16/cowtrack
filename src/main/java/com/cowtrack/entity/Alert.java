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

    public enum AlertType {
        GEOFENCE_BREACH,
        NO_SIGNAL,
        NIGHT_MOVEMENT,
        DEVICE_REMOVED
    }
}