package com.cowtrack.util;

/**
 * Decides when a new position means an animal has really changed sides of a
 * fence, as opposed to its collar's GPS wandering across the line.
 *
 * <p>Cattle graze right along fence lines, and a collar fix is typically good to
 * 10-30 m. Taking each fix at face value would raise and clear an alert every
 * few positions for an animal that never left. So a change of side is only
 * believed when it is either
 * <ul>
 *   <li><b>clear</b>: the fix is further over the line than its own accuracy
 *       plus a buffer, so no plausible error puts it on the other side; or</li>
 *   <li><b>repeated</b>: enough fixes in a row have landed on the other side.</li>
 * </ul>
 * The same rule applies in both directions, so an animal standing just outside
 * does not flicker between "left" and "returned" either.
 *
 * <p>Pure: no Spring, no entities, so the rule can be tested on its own.
 */
public final class FenceCrossing {

    /** What a fix did to the alert state of one animal against one fence. */
    public enum Event {
        /** Nothing an alert needs to hear about. */
        NONE,
        /** Now confirmed on the wrong side: outside a camp, or inside a restricted zone. */
        BREACH,
        /** Now confirmed back on the right side. */
        CLEARED
    }

    /**
     * Where an animal stands relative to a fence.
     *
     * @param inside       the side last confirmed
     * @param pendingFixes how many fixes in a row have disagreed with it since
     */
    public record State(boolean inside, int pendingFixes) {
    }

    public record Outcome(State next, Event event) {
    }

    private FenceCrossing() {
    }

    /**
     * @param keepIn          true for a camp (stay inside), false for a restricted
     *                        zone (stay out)
     * @param previous        the state before this fix, or null if this animal
     *                        has never been evaluated against this fence
     * @param signedDistance  metres from the boundary, negative inside
     * @param accuracyMeters  the collar's reported accuracy for this fix; 0 when
     *                        it does not say
     * @param bufferMeters    extra margin on top of accuracy before a single fix
     *                        is believed
     * @param confirmFixes    fixes in a row that confirm a change on their own
     */
    public static Outcome evaluate(boolean keepIn, State previous, double signedDistance,
                                   double accuracyMeters, double bufferMeters, int confirmFixes) {
        // With no history, the animal is assumed to be where it should be. A
        // first fix that is clearly on the wrong side still alerts below; one
        // that is only marginally wrong has to be repeated, like any other.
        State current = previous != null ? previous : new State(keepIn, 0);

        boolean rawInside = signedDistance <= 0;
        if (rawInside == current.inside()) {
            return new Outcome(new State(current.inside(), 0), Event.NONE);
        }

        int pending = current.pendingFixes() + 1;
        double margin = Math.max(0, accuracyMeters) + Math.max(0, bufferMeters);
        boolean clear = Math.abs(signedDistance) > margin;

        if (!clear && pending < Math.max(1, confirmFixes)) {
            return new Outcome(new State(current.inside(), pending), Event.NONE);
        }

        boolean wrongSide = rawInside != keepIn;
        return new Outcome(new State(rawInside, 0), wrongSide ? Event.BREACH : Event.CLEARED);
    }
}
