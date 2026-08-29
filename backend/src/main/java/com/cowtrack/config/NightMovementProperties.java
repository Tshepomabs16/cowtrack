package com.cowtrack.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;

/**
 * Tuning for night-movement detection, under
 * {@code cowtrack.monitoring.night-movement.*}.
 *
 * <p>Cattle rest at night. Sustained movement in the small hours is the
 * signature of an animal being driven off, which is why this is worth alerting
 * on at all — and why the distance threshold has to be high enough to ignore an
 * animal shifting position at a water trough.
 */
@Data
@Component
@ConfigurationProperties(prefix = "cowtrack.monitoring.night-movement")
public class NightMovementProperties {

    private boolean enabled = true;

    /** Hour the night window opens, inclusive, in {@link #zone}. */
    private int startHour = 22;

    /** Hour the night window closes, exclusive, in {@link #zone}. */
    private int endHour = 5;

    /** Distance covered within {@link #window} that counts as being driven. */
    private double thresholdMetres = 100;

    /** How far back to sum movement when a position arrives. */
    private Duration window = Duration.ofHours(1);

    /**
     * The zone the night window is interpreted in.
     *
     * <p>Defaults to the server's zone, which is only correct while every farm
     * shares it. This belongs on the farm rather than in configuration — a
     * deployment serving farms in different zones would call the same clock hour
     * "night" for all of them — but that needs a schema change, so it is a
     * single setting for now and wrong for a multi-region deployment.
     */
    private ZoneId zone = ZoneId.systemDefault();
}
