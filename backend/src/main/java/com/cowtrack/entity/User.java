package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    @EqualsAndHashCode.Include
    private Long userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column
    private String phone;

    @Column(name = "farm_name")
    private String farmName;

    @Column
    private String location;

    @Column
    private String timezone;

    @Column
    private String language;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_farms",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "farm_id")
    )
    private Set<Farm> farms = new HashSet<>();

    public enum Role {
        FARMER, CARETAKER, ADMIN
    }

    /**
     * Returns the primary farm for this user.
     * For multi-farm support, this would need to be extended with context.
     */
    @Transient
    public Farm getPrimaryFarm() {
        if (farms == null || farms.isEmpty()) {
            return null;
        }
        return farms.iterator().next();
    }

    /**
     * Returns the primary farm ID, or null if the user has no farms.
     */
    @Transient
    public Long getPrimaryFarmId() {
        Farm farm = getPrimaryFarm();
        return farm != null ? farm.getFarmId() : null;
    }
}