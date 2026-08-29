package com.cowtrack.util;

import com.cowtrack.entity.Cow;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class AlertMessageGenerator {

    public String generateGeofenceBreachMessage(Cow cow, boolean isInside) {
        String action = isInside ? "entered" : "exited";
        return String.format("Cow '%s' (Tag: %s) has %s the geofence area at %s",
                cow.getName(),
                cow.getTagId(),
                action,
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    public String generateNoSignalMessage(Cow cow, long hours) {
        return String.format("Cow '%s' (Tag: %s) has had no GPS signal for %s",
                cow.getName(),
                cow.getTagId(),
                describeSilence(hours));
    }

    /**
     * The silence threshold is configurable and may be shorter than an hour for
     * frequently-reporting collars, in which case whole hours truncate to zero
     * and the alert reads "no GPS signal for 0 hours". Days are spelled out too,
     * since "for 73 hours" is harder to act on than "for 3 days".
     */
    private String describeSilence(long hours) {
        if (hours < 1) {
            return "under an hour";
        }
        if (hours < 48) {
            return hours + (hours == 1 ? " hour" : " hours");
        }
        long days = hours / 24;
        return days + " days";
    }

    public String generateNightMovementMessage(Cow cow) {
        return String.format("Cow '%s' (Tag: %s) is moving during night hours",
                cow.getName(),
                cow.getTagId());
    }

    public String generateDeviceRemovedMessage(Cow cow) {
        return String.format("Possible device removal detected for cow '%s' (Tag: %s)",
                cow.getName(),
                cow.getTagId());
    }
}