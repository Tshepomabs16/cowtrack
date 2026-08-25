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

    @Column(name = "tag_id", nullable = false, unique = true)
    private String tagId;

    @Column(nullable = false)
    private String name;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // Mother relationship (self-referencing)
    @ManyToOne
    @JoinColumn(name = "mother_id")
    private Cow mother;

    // Father relationship (self-referencing)
    @ManyToOne
    @JoinColumn(name = "father_id")
    private Cow father;

    // Caretaker relationship
    @ManyToOne
    @JoinColumn(name = "caretaker_id")
    private User caretaker;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}