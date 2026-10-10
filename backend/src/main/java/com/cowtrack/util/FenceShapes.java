package com.cowtrack.util;

import com.cowtrack.entity.Geofence;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts between a stored {@link Geofence} and the geometry in
 * {@link GeofenceUtils}: polygon corners in and out of their JSON column, and
 * the signed distance from a fence to a point whatever its shape.
 */
public final class FenceShapes {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<List<BigDecimal>>> VERTICES = new TypeReference<>() {
    };

    private FenceShapes() {
    }

    /**
     * Metres from the fence's boundary to the point, negative inside.
     *
     * @throws IllegalStateException if the stored shape is unusable, which
     *                               validation on the way in should make impossible
     */
    public static double signedDistanceMeters(Geofence fence, double latitude, double longitude) {
        if (fence.getShape() == Geofence.Shape.POLYGON) {
            List<double[]> corners = corners(fence.getVerticesJson());
            if (corners.size() < 3) {
                throw new IllegalStateException("Fence " + fence.getGeofenceId() + " has no usable corners");
            }
            return GeofenceUtils.signedDistanceToPolygonMeters(corners, latitude, longitude);
        }
        if (fence.getCenterLatitude() == null || fence.getCenterLongitude() == null
                || fence.getRadiusMeters() == null) {
            throw new IllegalStateException("Fence " + fence.getGeofenceId() + " has no centre or radius");
        }
        return GeofenceUtils.signedDistanceToCircleMeters(latitude, longitude,
                fence.getCenterLatitude().doubleValue(), fence.getCenterLongitude().doubleValue(),
                fence.getRadiusMeters());
    }

    /** Stored corners as {@code [lat, lng]} pairs; empty if there are none. */
    public static List<double[]> corners(String verticesJson) {
        List<double[]> out = new ArrayList<>();
        for (List<BigDecimal> vertex : vertices(verticesJson)) {
            out.add(new double[] {vertex.get(0).doubleValue(), vertex.get(1).doubleValue()});
        }
        return out;
    }

    /** Stored corners as the API sends them; empty if there are none. */
    public static List<List<BigDecimal>> vertices(String verticesJson) {
        if (verticesJson == null || verticesJson.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(verticesJson, VERTICES);
        } catch (Exception e) {
            throw new IllegalStateException("Stored polygon corners could not be read", e);
        }
    }

    public static String toJson(List<List<BigDecimal>> vertices) {
        try {
            return JSON.writeValueAsString(vertices);
        } catch (Exception e) {
            throw new IllegalArgumentException("Polygon corners could not be stored", e);
        }
    }
}
