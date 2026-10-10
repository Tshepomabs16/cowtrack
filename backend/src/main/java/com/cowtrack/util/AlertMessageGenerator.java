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

    /**
     * Says which fence and by how far, because that decides the response: an
     * animal 20 m through a fence is a walk, one 2 km away is a vehicle.
     */
    public String generateFenceBreachMessage(Cow cow, String fenceName, boolean keepIn, double metresOver) {
        String what = keepIn
                ? String.format("has left camp '%s' and is %s outside it", fenceName, describeDistance(metresOver))
                : String.format("has entered restricted area '%s', %s inside it", fenceName, describeDistance(metresOver));
        return fit(String.format("Cow '%s' (Tag: %s) %s", cow.getName(), cow.getTagId(), what));
    }

    /** The note left on a breach alert the system closed when the animal came back. */
    public String generateFenceClearedNote(String fenceName, boolean keepIn, java.time.LocalDateTime at) {
        String what = keepIn ? "Returned inside camp '%s'" : "Left restricted area '%s'";
        return fit(String.format(what + " at %s", fenceName,
                at.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
    }

    /** The alerts table holds 255 characters; a long animal or camp name must not fail the insert. */
    private static String fit(String message) {
        return message.length() <= 255 ? message : message.substring(0, 254) + "…";
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

    /**
     * Carries the distance, because that is what tells a farmer whether to get
     * out of bed: cattle shuffling around a trough read very differently from an
     * animal that has covered half a kilometre in the dark.
     */
    public String generateNightMovementMessage(Cow cow, double metres) {
        return String.format("Cow '%s' (Tag: %s) moved %s during night hours",
                cow.getName(),
                cow.getTagId(),
                describeDistance(metres));
    }

    private String describeDistance(double metres) {
        if (metres >= 1000) {
            return String.format("%.1f km", metres / 1000);
        }
        return Math.round(metres) + " m";
    }

    /** Names the collar as well as the animal: it is the collar that needs swapping. */
    public String generateLowBatteryMessage(Cow cow, String serialNumber, Integer batteryPercent) {
        return String.format("Collar %s on '%s' (Tag: %s) is at %d%% battery",
                serialNumber,
                cow.getName(),
                cow.getTagId(),
                batteryPercent == null ? 0 : batteryPercent);
    }

    public String generateDeviceRemovedMessage(Cow cow) {
        return String.format("Possible device removal detected for cow '%s' (Tag: %s)",
                cow.getName(),
                cow.getTagId());
    }
}