package com.cowtrack.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * How sure the system has to be before it believes an animal crossed a fence,
 * under {@code cowtrack.geofence.*}. See {@link com.cowtrack.util.FenceCrossing}.
 *
 * <p>Server-wide for now. These belong on the farm alongside the other tracking
 * rules, since a farm with better collars can afford a tighter margin.
 */
@Data
@Component
@ConfigurationProperties(prefix = "cowtrack.geofence")
public class GeofenceProperties {

    /**
     * Metres beyond the collar's own reported accuracy that one fix must be over
     * the line to count by itself. Covers cattle grazing along a fence and
     * collars that understate their error.
     */
    private double boundaryBufferMetres = 15;

    /** Fixes in a row over the line that count as a crossing however close they are. */
    private int confirmFixes = 2;
}
