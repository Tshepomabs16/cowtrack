package com.cowtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A tracking collar, and the record of what it last told us about itself.
 *
 * <p>Modelled separately from the animal because the two have different
 * lifecycles: a collar is bought, fitted, moved between animals, flattens and is
 * replaced. Folding its battery and firmware onto {@link Cow} would lose that
 * history the moment the collar changed animals, and would leave a collar
 * sitting in a store with nowhere to exist.
 *
 * <p>A device with no {@link #cow} is provisioned but not yet fitted. It can
 * still authenticate — so that a technician can confirm a collar works before
 * putting it on an animal — but its readings are rejected, since a position with
 * no animal attached means nothing.
 */
@Entity
@Table(
        name = "devices",
        uniqueConstraints = @UniqueConstraint(name = "uk_devices_serial", columnNames = "serial_number")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    /** Below this, the collar is close enough to dying to be worth acting on. */
    public static final int LOW_BATTERY_PERCENT = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long deviceId;

    /** What the hardware calls itself. Printed on the collar. */
    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /** Null while the collar is provisioned but not yet fitted to an animal. */
    @ManyToOne
    @JoinColumn(name = "cow_id")
    private Cow cow;

    /**
     * BCrypt hash of the key the device presents. Stored hashed for the same
     * reason a password is: a leaked database should not hand over the ability to
     * forge positions for a whole herd.
     */
    @Column(name = "api_key_hash", nullable = false)
    private String apiKeyHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    /** Last reported charge, 0-100. Null until the collar first reports. */
    @Column(name = "battery_percent")
    private Integer batteryPercent;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    /**
     * When the device last contacted us, as distinct from the timestamp on its
     * readings. A collar that has buffered for two days and then uploads is
     * healthy; one whose readings are current but which has not called in is not.
     */
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** True once the collar has reported a charge at or below the threshold. */
    @Transient
    public boolean isBatteryLow() {
        return batteryPercent != null && batteryPercent <= LOW_BATTERY_PERCENT;
    }

    public enum Status {
        /** Fitted or ready to be; readings accepted. */
        ACTIVE,
        /** Withdrawn from service; readings refused without deleting its history. */
        RETIRED
    }
}
