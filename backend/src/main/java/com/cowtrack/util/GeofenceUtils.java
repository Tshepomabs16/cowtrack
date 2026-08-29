package com.cowtrack.util;

/**
 * Pure spherical geometry for geofencing. Kept free of Spring and entity
 * dependencies so the calculations can be unit-tested in isolation.
 */
public final class GeofenceUtils {

    /** Mean Earth radius in metres, the value used by the haversine formula. */
    public static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeofenceUtils() {
    }

    /**
     * Great-circle distance between two points on the WGS-84 sphere using the
     * haversine formula.
     *
     * @param lat1Degrees latitude of the first point, in degrees
     * @param lon1Degrees longitude of the first point, in degrees
     * @param lat2Degrees latitude of the second point, in degrees
     * @param lon2Degrees longitude of the second point, in degrees
     * @return distance in metres
     */
    public static double haversineDistanceMeters(
            double lat1Degrees, double lon1Degrees, double lat2Degrees, double lon2Degrees) {
        double dLat = Math.toRadians(lat2Degrees - lat1Degrees);
        double dLon = Math.toRadians(lon2Degrees - lon1Degrees);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1Degrees))
                * Math.cos(Math.toRadians(lat2Degrees))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Whether a point lies within a circular geofence. A point exactly on the
     * boundary counts as inside.
     *
     * @param latDegrees   the cow's latitude, in degrees
     * @param lonDegrees   the cow's longitude, in degrees
     * @param centerLatDegrees the geofence centre latitude, in degrees
     * @param centerLonDegrees the geofence centre longitude, in degrees
     * @param radiusMeters the geofence radius, in metres
     * @return {@code true} when the distance to the centre does not exceed the radius
     */
    public static boolean isInsideRadius(
            double latDegrees, double lonDegrees,
            double centerLatDegrees, double centerLonDegrees, double radiusMeters) {
        double distance = haversineDistanceMeters(
                latDegrees, lonDegrees, centerLatDegrees, centerLonDegrees);
        return distance <= radiusMeters;
    }
}