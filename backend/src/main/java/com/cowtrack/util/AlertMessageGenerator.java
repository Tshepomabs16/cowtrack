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
        return String.format("Cow '%s' (Tag: %s) has had no GPS signal for %d hours",
                cow.getName(),
                cow.getTagId(),
                hours);
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