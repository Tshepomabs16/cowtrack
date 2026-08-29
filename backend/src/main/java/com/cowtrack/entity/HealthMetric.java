package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetric {

    public static final BigDecimal TEMPERATURE_WARNING_THRESHOLD = new BigDecimal("38.5");
    public static final BigDecimal HEART_RATE_WARNING_THRESHOLD = new BigDecimal("70");
    public static final BigDecimal ACTIVITY_WARNING_THRESHOLD = new BigDecimal("50");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private Long metricId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cow_id", nullable = false)
    private Cow cow;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "heart_rate", precision = 5, scale = 1)
    private BigDecimal heartRate;

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
