package com.cowtrack.util;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class GeofenceCalculator {

    private static final double EARTH_RADIUS_METERS = 6371000; // Earth's radius in meters

    /**
     * Calculate distance between two points using Haversine formula
     * @return distance in meters
     */
    public double calculateDistance(
            BigDecimal lat1, BigDecimal lng1,
            BigDecimal lat2, BigDecimal lng2) {

        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double latDiff = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double lngDiff = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());

        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Check if a point is inside a circular geofence
     */
    public boolean isInsideGeofence(
            BigDecimal pointLat, BigDecimal pointLng,
            BigDecimal centerLat, BigDecimal centerLng,
            int radiusMeters) {

        double distance = calculateDistance(pointLat, pointLng, centerLat, centerLng);
        return distance <= radiusMeters;
    }

    /**
     * Format distance for display
     */
    public String formatDistance(double meters) {
        if (meters < 1000) {
            return Math.round(meters) + "m";
        } else {
            double km = meters / 1000.0;
            return BigDecimal.valueOf(km).setScale(1, RoundingMode.HALF_UP) + "km";
        }
    }
}