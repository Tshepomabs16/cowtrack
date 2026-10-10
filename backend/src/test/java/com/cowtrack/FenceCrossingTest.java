package com.cowtrack;

import com.cowtrack.util.FenceCrossing;
import com.cowtrack.util.FenceCrossing.Event;
import com.cowtrack.util.FenceCrossing.Outcome;
import com.cowtrack.util.FenceCrossing.State;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rule that decides when a position means an animal really crossed a
 * fence. Distances are signed: negative inside, positive outside.
 *
 * <p>The cases that matter most are the ones that must <em>not</em> alert: a
 * cow grazing along a fence line with a collar that wanders by 10-30 m would
 * otherwise raise and clear alerts all day.
 */
class FenceCrossingTest {

    private static final double BUFFER = 15;
    private static final int CONFIRM = 2;
    private static final boolean CAMP = true;
    private static final boolean RESTRICTED = false;

    private static Outcome camp(State previous, double distance, double accuracy) {
        return FenceCrossing.evaluate(CAMP, previous, distance, accuracy, BUFFER, CONFIRM);
    }

    private static final State INSIDE = new State(true, 0);
    private static final State OUTSIDE = new State(false, 0);

    @Test
    void anAnimalStayingInsideItsCampRaisesNothing() {
        Outcome outcome = camp(INSIDE, -200, 10);
        assertThat(outcome.event()).isEqualTo(Event.NONE);
        assertThat(outcome.next()).isEqualTo(INSIDE);
    }

    @Test
    void aFixClearlyOutsideTheCampIsABreachStraightAway() {
        // 300 m out with a 10 m accuracy: no error puts it inside.
        Outcome outcome = camp(INSIDE, 300, 10);
        assertThat(outcome.event()).isEqualTo(Event.BREACH);
        assertThat(outcome.next()).isEqualTo(OUTSIDE);
    }

    @Test
    void oneFixJustOverTheLineIsNotBelievedOnItsOwn() {
        // 12 m out, but the collar says it is only good to 10 m, plus the buffer.
        Outcome outcome = camp(INSIDE, 12, 10);
        assertThat(outcome.event()).isEqualTo(Event.NONE);
        assertThat(outcome.next()).isEqualTo(new State(true, 1));
    }

    @Test
    void aSecondFixInARowJustOverTheLineConfirmsTheBreach() {
        Outcome first = camp(INSIDE, 12, 10);
        Outcome second = camp(first.next(), 8, 10);
        assertThat(second.event()).isEqualTo(Event.BREACH);
        assertThat(second.next()).isEqualTo(OUTSIDE);
    }

    @Test
    void grazingAlongTheFenceLineNeverAlerts() {
        // Fixes alternating either side of the line, as a collar wanders.
        double[] wander = {-5, 8, -3, 12, -6, 4, -2, 9};
        State state = INSIDE;
        for (double distance : wander) {
            Outcome outcome = camp(state, distance, 10);
            assertThat(outcome.event()).as("fix at %s m", distance).isEqualTo(Event.NONE);
            state = outcome.next();
        }
        assertThat(state.inside()).isTrue();
    }

    @Test
    void aPoorFixNeedsToRepeatHoweverFarOutItLooks() {
        // 60 m out, but the collar admits a 100 m error.
        Outcome outcome = camp(INSIDE, 60, 100);
        assertThat(outcome.event()).isEqualTo(Event.NONE);
    }

    @Test
    void comingBackInsideClearsTheBreachRatherThanRaisingAnother() {
        Outcome outcome = camp(OUTSIDE, -200, 10);
        assertThat(outcome.event()).isEqualTo(Event.CLEARED);
        assertThat(outcome.next()).isEqualTo(INSIDE);
    }

    @Test
    void anAnimalHoveringJustInsideAfterABreachIsNotClearedByOneFix() {
        // The same margin applies on the way back, so a cow standing at the
        // fence does not flicker between "left" and "returned".
        Outcome outcome = camp(OUTSIDE, -5, 10);
        assertThat(outcome.event()).isEqualTo(Event.NONE);
        assertThat(outcome.next()).isEqualTo(new State(false, 1));
    }

    @Test
    void stayingOutsideDoesNotRaiseTheBreachAgain() {
        Outcome outcome = camp(OUTSIDE, 500, 10);
        assertThat(outcome.event()).isEqualTo(Event.NONE);
    }

    @Test
    void aFirstFixInsideTheCampIsJustTheBaseline() {
        Outcome outcome = camp(null, -100, 10);
        assertThat(outcome.event()).isEqualTo(Event.NONE);
        assertThat(outcome.next()).isEqualTo(INSIDE);
    }

    @Test
    void aFirstFixClearlyOutsideTheCampStillAlerts() {
        // An animal put in a camp it is not actually in should be noticed.
        Outcome outcome = camp(null, 400, 10);
        assertThat(outcome.event()).isEqualTo(Event.BREACH);
    }

    @Test
    void enteringARestrictedZoneIsABreachAndLeavingItClears() {
        Outcome entered = FenceCrossing.evaluate(RESTRICTED, null, -80, 10, BUFFER, CONFIRM);
        assertThat(entered.event()).isEqualTo(Event.BREACH);
        assertThat(entered.next().inside()).isTrue();

        Outcome left = FenceCrossing.evaluate(RESTRICTED, entered.next(), 80, 10, BUFFER, CONFIRM);
        assertThat(left.event()).isEqualTo(Event.CLEARED);
    }

    @Test
    void stayingClearOfARestrictedZoneRaisesNothing() {
        Outcome outcome = FenceCrossing.evaluate(RESTRICTED, null, 300, 10, BUFFER, CONFIRM);
        assertThat(outcome.event()).isEqualTo(Event.NONE);
        assertThat(outcome.next()).isEqualTo(OUTSIDE);
    }

    @Test
    void aMissingAccuracyIsTreatedAsExactNotAsUseless() {
        // 20 m out with no reported accuracy clears the 15 m buffer alone.
        assertThat(camp(INSIDE, 20, 0).event()).isEqualTo(Event.BREACH);
    }

    @Test
    void confirmingOnOneFixMeansEveryCrossingCounts() {
        Outcome outcome = FenceCrossing.evaluate(CAMP, INSIDE, 3, 10, BUFFER, 1);
        assertThat(outcome.event()).isEqualTo(Event.BREACH);
    }
}
