package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cow_id", nullable = false)
    private Cow cow;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "milk_litres", precision = 8, scale = 2)
    private BigDecimal milkLitres;

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
