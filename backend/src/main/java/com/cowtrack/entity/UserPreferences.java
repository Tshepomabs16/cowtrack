package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Per-user display and notification settings, backing the settings page.
 *
 * <p>Defaults are set on the fields so a user who has never saved preferences still
 * gets a complete object rather than a wall of nulls.
 */
@Entity
@Table(name = "user_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preferences_id")
    private Long preferencesId;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column
    private String theme = "light";

    @Column
    private String language = "en";

    @Column
    private String timezone = "UTC";

    /** "metric" or "imperial". */
    @Column
    private String units = "metric";

    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;

    @Column(name = "push_notifications")
    private Boolean pushNotifications = true;

    @Column(name = "alert_notifications")
    private Boolean alertNotifications = true;

    @Column(name = "weekly_reports")
    private Boolean weeklyReports = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = LocalDateTime.now();
    }

    public static UserPreferences defaultsFor(User user) {
        UserPreferences preferences = new UserPreferences();
        preferences.setUser(user);
        return preferences;
    }
}
