package com.cowtrack.util;

import java.util.List;

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

    /** Within this of a polygon's edge, a point is on the edge, and the edge is part of the shape. */
    public static final double EDGE_TOLERANCE_METERS = 0.5;

    /**
     * How far a point is from a circle's boundary: negative inside, positive
     * outside, zero on the line. The sign answers "which side", the magnitude
     * answers "by how much", which is what a farmer needs to judge an alert.
     */
    public static double signedDistanceToCircleMeters(
            double latDegrees, double lonDegrees,
            double centerLatDegrees, double centerLonDegrees, double radiusMeters) {
        return haversineDistanceMeters(latDegrees, lonDegrees, centerLatDegrees, centerLonDegrees)
                - radiusMeters;
    }

    /**
     * How far a point is from a polygon's boundary: negative inside, positive
     * outside. A point within {@link #EDGE_TOLERANCE_METERS} of an edge counts as
     * inside.
     *
     * <p>Distances use a flat projection centred on the point, which is accurate
     * to well under a metre across anything the size of a farm.
     *
     * @param vertices corners as {@code [lat, lon]} pairs, in drawing order; the
     *                 shape closes itself, so the first corner is not repeated
     */
    public static double signedDistanceToPolygonMeters(
            List<double[]> vertices, double latDegrees, double lonDegrees) {
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("A polygon needs at least three corners");
        }
        double nearest = Double.MAX_VALUE;
        double[] origin = {0, 0};
        for (int i = 0; i < vertices.size(); i++) {
            double[] a = project(vertices.get(i), latDegrees, lonDegrees);
            double[] b = project(vertices.get((i + 1) % vertices.size()), latDegrees, lonDegrees);
            nearest = Math.min(nearest, distanceToSegment(origin, a, b));
        }
        if (nearest <= EDGE_TOLERANCE_METERS) {
            return -nearest;
        }
        return isInsidePolygon(vertices, latDegrees, lonDegrees) ? -nearest : nearest;
    }

    /** Ray casting; corners are {@code [lat, lon]} pairs. */
    public static boolean isInsidePolygon(List<double[]> vertices, double latDegrees, double lonDegrees) {
        boolean inside = false;
        for (int i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
            double latI = vertices.get(i)[0], lonI = vertices.get(i)[1];
            double latJ = vertices.get(j)[0], lonJ = vertices.get(j)[1];
            boolean straddles = (lonI > lonDegrees) != (lonJ > lonDegrees);
            if (straddles && latDegrees < (latJ - latI) * (lonDegrees - lonI) / (lonJ - lonI) + latI) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * Whether any two non-adjacent edges of a closed polygon cross. A camp
     * drawn as a figure eight has no sensible inside, so it is refused rather
     * than evaluated.
     */
    public static boolean polygonSelfIntersects(List<double[]> vertices) {
        int n = vertices.size();
        if (n < 4) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            double[] a = vertices.get(i);
            double[] b = vertices.get((i + 1) % n);
            for (int j = i + 2; j < n; j++) {
                // The first and last edges share a corner.
                if (i == 0 && j == n - 1) {
                    continue;
                }
                if (segmentsIntersect(a, b, vertices.get(j), vertices.get((j + 1) % n))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double[] project(double[] vertex, double originLat, double originLon) {
        double metresPerDegree = Math.PI * EARTH_RADIUS_METERS / 180.0;
        return new double[] {
                (vertex[1] - originLon) * metresPerDegree * Math.cos(Math.toRadians(originLat)),
                (vertex[0] - originLat) * metresPerDegree
        };
    }

    private static double distanceToSegment(double[] p, double[] a, double[] b) {
        double dx = b[0] - a[0], dy = b[1] - a[1];
        double lengthSquared = dx * dx + dy * dy;
        double t = lengthSquared == 0 ? 0 : ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(p[0] - (a[0] + t * dx), p[1] - (a[1] + t * dy));
    }

    private static boolean segmentsIntersect(double[] a, double[] b, double[] c, double[] d) {
        int o1 = orientation(a, b, c);
        int o2 = orientation(a, b, d);
        int o3 = orientation(c, d, a);
        int o4 = orientation(c, d, b);
        if (o1 != o2 && o3 != o4 && o1 != 0 && o2 != 0 && o3 != 0 && o4 != 0) {
            return true;
        }
        return (o1 == 0 && onSegment(a, b, c))
                || (o2 == 0 && onSegment(a, b, d))
                || (o3 == 0 && onSegment(c, d, a))
                || (o4 == 0 && onSegment(c, d, b));
    }

    private static int orientation(double[] p, double[] q, double[] r) {
        double cross = (q[0] - p[0]) * (r[1] - p[1]) - (q[1] - p[1]) * (r[0] - p[0]);
        return Math.abs(cross) < 1e-18 ? 0 : (cross > 0 ? 1 : -1);
    }

    private static boolean onSegment(double[] p, double[] q, double[] r) {
        double epsilon = 1e-12;
        return Math.min(p[0], q[0]) - epsilon <= r[0] && r[0] <= Math.max(p[0], q[0]) + epsilon
                && Math.min(p[1], q[1]) - epsilon <= r[1] && r[1] <= Math.max(p[1], q[1]) + epsilon;
    }
}