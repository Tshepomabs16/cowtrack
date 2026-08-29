package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vaccinations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vaccination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vaccination_id")
    private Long vaccinationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cow_id", nullable = false)
    private Cow cow;

    @Column(name = "vaccine_name", nullable = false)
    private String vaccineName;

    @Column(name = "administered_date")
    private LocalDate administeredDate;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "vet_name")
    private String vetName;

    @Column
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Transient
    public Status getStatus() {
        if (administeredDate == null) {
            return nextDueDate != null && nextDueDate.isBefore(LocalDate.now())
                    ? Status.OVERDUE
                    : Status.SCHEDULED;
        }
        if (nextDueDate != null && nextDueDate.isBefore(LocalDate.now())) {
            return Status.OVERDUE;
        }
        return Status.COMPLETED;
    }

    public enum Status {
        SCHEDULED, COMPLETED, OVERDUE
    }
}
