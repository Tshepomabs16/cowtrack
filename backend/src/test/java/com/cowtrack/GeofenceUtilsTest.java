package com.cowtrack;

import com.cowtrack.util.GeofenceUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The geofence maths the alerting path leans on: a cow reported far from its
 * geofence centre must be flagged, one near it must not. The assertions rely
 * on known great-circle distances that are independent of this implementation.
 */
class GeofenceUtilsTest {

    private static final double TOLERANCE_METRES = 1_500.0;

    @Test
    void distanceBetweenIdenticalPointsIsZero() {
        double metres = GeofenceUtils.haversineDistanceMeters(-26.2041, 28.0473, -26.2041, 28.0473);
        assertThat(metres).isEqualTo(0.0);
    }

    /**
     * One degree of latitude is a fixed 111,195 m at any longitude. This checks
     * both the scale of the result and that it is not a degrees-as-meters bug.
     */
    @Test
    void oneDegreeOfLatitudeIsAboutOneHundredElevenKilometres() {
        double metres = GeofenceUtils.haversineDistanceMeters(0.0, 0.0, 1.0, 0.0);
        assertThat(metres).isBetween(111_000.0, 111_400.0);
    }

    /**
     * One degree of longitude at the equator is 111,320 m: the cosine-of-latitude
     * term must not accidentally zero that out.
     */
    @Test
    void oneDegreeOfLongitudeAtEquatorMatchesExpected() {
        double metres = GeofenceUtils.haversineDistanceMeters(0.0, 0.0, 0.0, 1.0);
        assertThat(metres).isBetween(111_100.0, 111_500.0);
    }

    /**
     * Sanity-check against a real pair of farms: Johannesburg to Cape Town.
     */
    @Test
    void johannesburgToCapeTownIsAroundTwelveHundredKilometres() {
        double metres = GeofenceUtils.haversineDistanceMeters(
                -26.2041028, 28.0473051, -33.9248686, 18.4240553);
        assertThat(metres).isBetween(1_260_000.0, 1_290_000.0);
    }

    @Test
    void distanceIsSymmetric() {
        double oneWay = GeofenceUtils.haversineDistanceMeters(-26.2, 28.0, -33.9, 18.4);
        double thereAndBack = GeofenceUtils.haversineDistanceMeters(-33.9, 18.4, -26.2, 28.0);
        assertThat(oneWay).isEqualTo(thereAndBack);
    }

    @Test
    void pointInsideGeofenceIsAllowed() {
        // Cow 1 km north of the fence centre, fence radius 1.5 km.
        assertThat(GeofenceUtils.isInsideRadius(
                -26.2041 + 0.009, 28.0473, -26.2041, 28.0473, 1_500.0)).isTrue();
    }

    @Test
    void pointJustOutsideGeofenceIsFlagged() {
        // Cow 2 km north of the fence centre, fence radius 1.5 km.
        assertThat(GeofenceUtils.isInsideRadius(
                -26.2041 + 0.018, 28.0473, -26.2041, 28.0473, 1_500.0)).isFalse();
    }

    @Test
    void pointExactlyOnBoundaryCountsAsInside() {
        double centreLat = -26.2041;
        double centreLon = 28.0473;
        double cowLat = centreLat + 0.009;
        double exactDistance = GeofenceUtils.haversineDistanceMeters(centreLat, centreLon, cowLat, centreLon);
        // "Inside" is distance <= radius, so a radius of exactly the measured
        // distance must still admit the point.
        assertThat(exactDistance).isGreaterThan(0.0);
        assertThat(GeofenceUtils.isInsideRadius(cowLat, centreLon, centreLat, centreLon, exactDistance)).isTrue();
        assertThat(GeofenceUtils.isInsideRadius(cowLat, centreLon, centreLat, centreLon, exactDistance - 0.001)).isFalse();
    }

    @Test
    void radiusOfZeroOnlyAllowsTheExactCentre() {
        assertThat(GeofenceUtils.isInsideRadius(1.0, 1.0, 1.0, 1.0, 0.0)).isTrue();
        assertThat(GeofenceUtils.isInsideRadius(1.0001, 1.0, 1.0, 1.0, 0.0)).isFalse();
    }

    @Test
    void antipodalPointsAreAboutTwentyThousandKilometresApart() {
        double halfTheGlobe = GeofenceUtils.haversineDistanceMeters(0.0, 0.0, 0.0, 180.0);
        assertThat(halfTheGlobe).isBetween(19_900_000.0, 20_100_000.0);
    }

    // --- polygons and signed distance ------------------------------------------

    /** A square camp roughly 1.1 km on a side, as [lat, lon] corners. */
    private static final List<double[]> SQUARE = List.of(
            new double[] {-23.900, 29.460},
            new double[] {-23.900, 29.470},
            new double[] {-23.910, 29.470},
            new double[] {-23.910, 29.460});

    /** One metre of latitude, in degrees. */
    private static final double METRE = 1.0 / 111_195.0;

    @Test
    void aPointInTheMiddleOfAPolygonIsInsideIt() {
        assertThat(GeofenceUtils.isInsidePolygon(SQUARE, -23.905, 29.465)).isTrue();
        assertThat(GeofenceUtils.signedDistanceToPolygonMeters(SQUARE, -23.905, 29.465)).isNegative();
    }

    @Test
    void aPointBeyondAPolygonIsOutsideItByTheRightDistance() {
        // 100 m north of the northern edge.
        double distance = GeofenceUtils.signedDistanceToPolygonMeters(SQUARE, -23.900 + 100 * METRE, 29.465);
        assertThat(distance).isBetween(99.0, 101.0);
    }

    @Test
    void insideDistanceIsToTheNearestEdgeNotTheFurthest() {
        // 50 m inside the western edge, far from the others.
        double distance = GeofenceUtils.signedDistanceToPolygonMeters(SQUARE, -23.905, 29.460 + 50 * METRE
                / Math.cos(Math.toRadians(23.905)));
        assertThat(distance).isBetween(-51.0, -49.0);
    }

    @Test
    void aPointOnAPolygonEdgeCountsAsInside() {
        assertThat(GeofenceUtils.signedDistanceToPolygonMeters(SQUARE, -23.900, 29.465)).isLessThanOrEqualTo(0.0);
    }

    /** An L-shaped camp: the notch between its arms is outside, though it sits within the outline's bounds. */
    @Test
    void theNotchOfAConcaveCampIsOutsideIt() {
        List<double[]> lShape = List.of(
                new double[] {-23.900, 29.460},
                new double[] {-23.900, 29.465},
                new double[] {-23.905, 29.465},
                new double[] {-23.905, 29.470},
                new double[] {-23.910, 29.470},
                new double[] {-23.910, 29.460});
        assertThat(GeofenceUtils.isInsidePolygon(lShape, -23.902, 29.468)).isFalse();
        assertThat(GeofenceUtils.isInsidePolygon(lShape, -23.908, 29.468)).isTrue();
    }

    @Test
    void signedDistanceToACircleIsNegativeInsideAndPositiveOutside() {
        double centreLat = -23.9045, centreLon = 29.4689;
        assertThat(GeofenceUtils.signedDistanceToCircleMeters(
                centreLat + 100 * METRE, centreLon, centreLat, centreLon, 300)).isBetween(-201.0, -199.0);
        assertThat(GeofenceUtils.signedDistanceToCircleMeters(
                centreLat + 400 * METRE, centreLon, centreLat, centreLon, 300)).isBetween(99.0, 101.0);
    }

    @Test
    void aFigureEightPolygonIsRecognisedAsCrossingItself() {
        List<double[]> bowTie = List.of(
                new double[] {-23.900, 29.460},
                new double[] {-23.910, 29.470},
                new double[] {-23.900, 29.470},
                new double[] {-23.910, 29.460});
        assertThat(GeofenceUtils.polygonSelfIntersects(bowTie)).isTrue();
        assertThat(GeofenceUtils.polygonSelfIntersects(SQUARE)).isFalse();
    }

    @Test
    void aTriangleCannotCrossItself() {
        assertThat(GeofenceUtils.polygonSelfIntersects(SQUARE.subList(0, 3))).isFalse();
    }
}
