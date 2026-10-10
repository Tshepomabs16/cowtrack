package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cows")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cow_id")
    private Long cowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(name = "tag_id", nullable = false, unique = true)
    private String tagId;

    @Column(nullable = false)
    private String name;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column
    private String breed;

    @ManyToOne
    @JoinColumn(name = "mother_id")
    private Cow mother;

    @ManyToOne
    @JoinColumn(name = "father_id")
    private Cow father;

    @ManyToOne
    @JoinColumn(name = "caretaker_id")
    private User caretaker;

    /**
     * The camp this animal is grazing in, or null if it has not been put in one.
     * An animal is in one camp at a time; moving it is a change of this field.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camp_id")
    private Geofence camp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}