package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A day's milk yield and weight for one animal. Aggregated into the productivity
 * trends shown on the analytics page.
 */
@Entity
@Table(
        name = "production_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cow_id", "record_date"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_id")
    private Long productionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cow_id", nullable = false)
    private Cow cow;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    /** Milk yield in litres for the day. */
    @Column(name = "milk_litres", precision = 8, scale = 2)
    private BigDecimal milkLitres;

    /** Recorded weight in kilograms. */
    @Column(name = "weight_kg", precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
