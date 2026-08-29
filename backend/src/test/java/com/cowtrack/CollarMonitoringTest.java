package com.cowtrack;

import com.cowtrack.entity.*;
import com.cowtrack.repository.*;
import com.cowtrack.service.CollarMonitoringService;
import com.cowtrack.service.impl.CollarMonitoringServiceImpl;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The no-signal sweep.
 *
 * <p>This detection previously hung off {@code recordLocation}, where it asked
 * whether a position had arrived in the last 24 hours immediately after one had.
 * It could not fire. The defining case here is therefore the one the old design
 * could not express: an animal that reports nothing at all still gets an alert,
 * because the sweep runs on a timer rather than on an incoming position.
 */
@SpringBootTest
@ActiveProfiles("test")
class CollarMonitoringTest {

    @Autowired private CollarMonitoringService collarMonitoring;
    @Autowired private com.cowtrack.service.AlertService alertService;
    @Autowired private FarmRepository farmRepository;
    @Autowired private CowRepository cowRepository;
    @Autowired private LocationRecordRepository locationRepository;
    @Autowired private AlertRepository alertRepository;
    @Autowired private ScheduledAnnotationBeanPostProcessor scheduledPostProcessor;

    @org.springframework.beans.factory.annotation.Value("${cowtrack.monitoring.no-signal.cron}")
    private String expectedCron;

    private Farm farm;

    /**
     * Each test gets its own farm and asserts only against it. Truncating the
     * shared tables instead would be both incomplete - other suites leave rows
     * in tables this class does not know about, and deleting cows violates their
     * foreign keys - and hostile to whatever else is using the context.
     */
    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setFarmName("Sweep Farm " + System.nanoTime());
        farm.setLocation("Limpopo");
        farm = farmRepository.save(farm);
    }

    private Cow cow(String tag) {
        Cow cow = new Cow();
        cow.setTagId(tag);
        cow.setName(tag);
        cow.setBreed("Nguni");
        cow.setDateOfBirth(LocalDate.now().minusYears(3));
        cow.setFarm(farm);
        cow.setCreatedAt(LocalDateTime.now().minusMonths(1));
        return cowRepository.save(cow);
    }

    private void positionAt(Cow cow, LocalDateTime when) {
        LocationRecord record = new LocationRecord();
        record.setCow(cow);
        record.setFarm(farm);
        record.setLatitude(new BigDecimal("-23.9045"));
        record.setLongitude(new BigDecimal("29.4689"));
        record.setAccuracy(BigDecimal.TEN);
        record.setRecordedAt(when);
        locationRepository.save(record);
    }

    /** No-signal alerts belonging to this test's farm only. */
    private List<Alert> noSignalAlerts() {
        return alertRepository.findAll().stream()
                .filter(alert -> alert.getAlertType() == Alert.AlertType.NO_SIGNAL)
                .filter(alert -> alert.getFarm() != null
                        && alert.getFarm().getFarmId().equals(farm.getFarmId()))
                .toList();
    }

    /** Alerts raised for this farm by one sweep, ignoring any other farm's. */
    private int sweepAndCountForThisFarm() {
        int before = noSignalAlerts().size();
        collarMonitoring.sweepForSilentCollars();
        return noSignalAlerts().size() - before;
    }

    /**
     * The case the previous design could not reach. No position arrives, so
     * nothing triggers an event; only a timer can notice.
     */
    @Test
    void raisesAnAlertForACollarThatHasGoneSilent() {
        Cow silent = cow("SWEEP-1");
        positionAt(silent, LocalDateTime.now().minusDays(3));

        assertThat(sweepAndCountForThisFarm()).isEqualTo(1);
        assertThat(noSignalAlerts())
                .singleElement()
                .satisfies(alert -> {
                    assertThat(alert.getCow().getTagId()).isEqualTo("SWEEP-1");
                    assertThat(alert.getIsResolved()).isFalse();
                    assertThat(alert.getFarm().getFarmId()).isEqualTo(farm.getFarmId());
                });
    }

    @Test
    void leavesACollarReportingWithinThresholdAlone() {
        positionAt(cow("SWEEP-2"), LocalDateTime.now().minusHours(2));

        assertThat(sweepAndCountForThisFarm()).isZero();
        assertThat(noSignalAlerts()).isEmpty();
    }

    /**
     * The sweep runs hourly against a condition that persists for as long as the
     * collar stays dead. Without de-duplication the alert feed would fill with
     * copies of the same unchanged fact.
     */
    @Test
    void doesNotRaiseASecondAlertWhileTheFirstIsStillOpen() {
        positionAt(cow("SWEEP-3"), LocalDateTime.now().minusDays(2));

        assertThat(sweepAndCountForThisFarm()).isEqualTo(1);
        assertThat(sweepAndCountForThisFarm()).isZero();
        assertThat(sweepAndCountForThisFarm()).isZero();

        assertThat(noSignalAlerts()).hasSize(1);
    }

    /**
     * Once the farmer has dealt with the alert, a collar that is still silent
     * should be reported again rather than staying quietly unmonitored.
     */
    @Test
    void raisesAgainAfterThePreviousAlertIsResolved() {
        positionAt(cow("SWEEP-4"), LocalDateTime.now().minusDays(2));
        collarMonitoring.sweepForSilentCollars();

        Alert open = noSignalAlerts().get(0);
        open.setIsResolved(true);
        alertRepository.save(open);

        assertThat(sweepAndCountForThisFarm()).isEqualTo(1);
        assertThat(noSignalAlerts()).hasSize(2);
    }

    /**
     * An animal with no position ever recorded has no collar fitted rather than a
     * failed one; alerting would be permanent and unactionable.
     */
    @Test
    void ignoresAnimalsThatHaveNeverReportedAPosition() {
        cow("SWEEP-5");

        assertThat(sweepAndCountForThisFarm()).isZero();
        assertThat(noSignalAlerts()).isEmpty();
    }

    /**
     * The sweep has no authenticated principal, so it must attribute each alert
     * to the right farm from the data rather than from a security context.
     */
    @Test
    void attributesEachAlertToTheOwningFarm() {
        Farm otherFarm = new Farm();
        otherFarm.setFarmName("Other Farm " + System.nanoTime());
        otherFarm.setLocation("Limpopo");
        otherFarm = farmRepository.save(otherFarm);

        Cow ours = cow("SWEEP-6");
        positionAt(ours, LocalDateTime.now().minusDays(2));

        Cow theirs = new Cow();
        theirs.setTagId("OTHER-1");
        theirs.setName("Theirs");
        theirs.setBreed("Angus");
        theirs.setDateOfBirth(LocalDate.now().minusYears(2));
        theirs.setFarm(otherFarm);
        theirs.setCreatedAt(LocalDateTime.now().minusMonths(1));
        theirs = cowRepository.save(theirs);

        LocationRecord theirRecord = new LocationRecord();
        theirRecord.setCow(theirs);
        theirRecord.setFarm(otherFarm);
        theirRecord.setLatitude(new BigDecimal("-24.1"));
        theirRecord.setLongitude(new BigDecimal("29.9"));
        theirRecord.setAccuracy(BigDecimal.TEN);
        theirRecord.setRecordedAt(LocalDateTime.now().minusDays(2));
        locationRepository.save(theirRecord);

        // Two farms are involved here, so both are counted deliberately.
        collarMonitoring.sweepForSilentCollars();

        List<Alert> raisedHere = alertRepository.findAll().stream()
                .filter(alert -> alert.getAlertType() == Alert.AlertType.NO_SIGNAL)
                .filter(alert -> List.of("SWEEP-6", "OTHER-1").contains(alert.getCow().getTagId()))
                .toList();

        assertThat(raisedHere).hasSize(2);

        for (Alert alert : raisedHere) {
            assertThat(alert.getFarm().getFarmId())
                    .withFailMessage("Alert for cow %s was attributed to farm %s but the animal belongs to farm %s",
                            alert.getCow().getTagId(), alert.getFarm().getFarmId(),
                            alert.getCow().getFarm().getFarmId())
                    .isEqualTo(alert.getCow().getFarm().getFarmId());
        }
    }

    /**
     * The original defect was structural rather than logical: the detection was
     * only ever invoked from the location-recording path, so it had no way to run
     * when nothing was arriving. Guarding the condition alone would not catch a
     * regression that removed or broke the trigger, so this asserts a cron task
     * is actually registered against the sweep. A malformed cron expression or a
     * missing @EnableScheduling would leave the sweep silently never running,
     * which is the exact failure being fixed.
     */
    @Test
    void theSweepIsRegisteredAsACronTask() {
        List<CronTask> cronTasks = scheduledPostProcessor.getScheduledTasks().stream()
                .map(ScheduledTask::getTask)
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .toList();

        // Matched on the task description rather than the runnable's type:
        // Spring wraps the scheduled method in an OutcomeTrackingRunnable, so it
        // is no longer a ScheduledMethodRunnable by the time it is registered.
        String sweepMethod = CollarMonitoringServiceImpl.class.getName() + ".scheduledSweep";

        CronTask sweep = cronTasks.stream()
                .filter(task -> task.toString().contains(sweepMethod))
                .findFirst()
                .orElse(null);

        assertThat(sweep)
                .withFailMessage("No cron task is registered for the collar sweep, so silent "
                        + "collars would never be detected. Registered cron tasks: %s", cronTasks)
                .isNotNull();

        assertThat(sweep.getExpression())
                .withFailMessage("The sweep is registered but on an unexpected schedule: %s",
                        sweep.getExpression())
                .isEqualTo(expectedCron);
    }

    /**
     * The threshold is configurable and may be shorter than an hour, in which
     * case whole-hour truncation produced "no GPS signal for 0 hours". Surfaced
     * by running the sweep with a one-minute threshold during verification.
     */
    @Test
    void describesTheSilenceInTermsAFarmerCanActOn() {
        Cow silent = cow("SWEEP-7");
        positionAt(silent, LocalDateTime.now().minusMinutes(30));

        // Sub-hour silence, reached by lowering the threshold below the gap.
        alertService.createNoSignalAlert(farm.getFarmId(), silent.getCowId(), 0);

        assertThat(noSignalAlerts())
                .singleElement()
                .satisfies(alert -> assertThat(alert.getMessage())
                        .doesNotContain("0 hours")
                        .contains("under an hour"));
    }
}
