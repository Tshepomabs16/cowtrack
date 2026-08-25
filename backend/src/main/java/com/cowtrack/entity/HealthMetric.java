package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A collar vitals reading. Drives the health trend charts and the per-animal
 * vitals table, which is why the thresholds used to classify a reading live here
 * rather than being duplicated across the UI.
 */
@Entity
@Table(name = "health_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetric {

    /** Above this body temperature (Celsius) a reading is considered a warning. */
    public static final BigDecimal TEMPERATURE_WARNING_THRESHOLD = new BigDecimal("38.5");

    /** Above this heart rate (bpm) a reading is considered a warning. */
    public static final BigDecimal HEART_RATE_WARNING_THRESHOLD = new BigDecimal("70");

    /** Below this activity percentage a reading is considered a warning. */
    public static final BigDecimal ACTIVITY_WARNING_THRESHOLD = new BigDecimal("50");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private Long metricId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cow_id", nullable = false)
    private Cow cow;

    /** Body temperature in degrees Celsius. */
    @Column(precision = 4, scale = 1)
    private BigDecimal temperature;

    /** Heart rate in beats per minute. */
    @Column(name = "heart_rate", precision = 5, scale = 1)
    private BigDecimal heartRate;

    /** Activity level as a percentage of the animal's normal daily movement. */
    @Column(name = "activity_level", precision = 5, scale = 1)
    private BigDecimal activityLevel;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }

    /** True when any vital sits outside its normal band. */
    @Transient
    public boolean isWarning() {
        return exceeds(temperature, TEMPERATURE_WARNING_THRESHOLD)
                || exceeds(heartRate, HEART_RATE_WARNING_THRESHOLD)
                || (activityLevel != null
                    && activityLevel.compareTo(ACTIVITY_WARNING_THRESHOLD) < 0);
    }

    private boolean exceeds(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) > 0;
    }
}
