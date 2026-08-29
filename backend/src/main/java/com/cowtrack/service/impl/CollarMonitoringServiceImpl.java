package com.cowtrack.service.impl;

import com.cowtrack.config.CollarMonitoringProperties;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Farm;
import com.cowtrack.entity.LocationRecord;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.FarmRepository;
import com.cowtrack.repository.LocationRecordRepository;
import com.cowtrack.service.AlertService;
import com.cowtrack.service.CollarMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollarMonitoringServiceImpl implements CollarMonitoringService {

    private final FarmRepository farmRepository;
    private final CowRepository cowRepository;
    private final LocationRecordRepository locationRecordRepository;
    private final AlertService alertService;
    private final CollarMonitoringProperties properties;

    /**
     * Runs on the schedule in {@code cowtrack.monitoring.no-signal.cron}.
     *
     * <p>Deliberately separate from {@link #sweepForSilentCollars()} so the sweep
     * can be invoked directly from a test or an operator endpoint without waiting
     * for the timer, and so the scheduling concern stays out of the logic.
     */
    @Scheduled(cron = "${cowtrack.monitoring.no-signal.cron:0 0 * * * *}")
    public void scheduledSweep() {
        if (!properties.isEnabled()) {
            log.debug("Collar monitoring disabled; skipping sweep");
            return;
        }
        int raised = sweepForSilentCollars();
        if (raised > 0) {
            log.info("Collar sweep raised {} no-signal alert(s)", raised);
        }
    }

    @Override
    @Transactional
    public int sweepForSilentCollars() {
        Duration threshold = properties.getThreshold();
        LocalDateTime cutoff = LocalDateTime.now().minus(threshold);
        int raised = 0;

        // Iterated per farm rather than across all cows at once: the sweep runs
        // on a scheduler with no authenticated principal, so there is no farm in
        // the security context to scope by. Each farm is named explicitly.
        for (Farm farm : farmRepository.findAll()) {
            Long farmId = farm.getFarmId();

            for (Cow cow : cowRepository.findByFarmFarmId(farmId)) {
                Optional<LocationRecord> latest =
                        locationRecordRepository.findLatestByFarmIdAndCowId(farmId, cow.getCowId());

                if (latest.isEmpty()) {
                    // No position ever recorded means no collar fitted, not a
                    // failed one. Alerting here would produce a permanent,
                    // unactionable alert for every untagged animal.
                    if (!properties.isIgnoreNeverReported()) {
                        raised += raise(farmId, cow, threshold.toHours());
                    }
                    continue;
                }

                LocalDateTime lastHeard = latest.get().getRecordedAt();
                if (lastHeard.isBefore(cutoff)) {
                    long silentHours = Duration.between(lastHeard, LocalDateTime.now()).toHours();
                    raised += raise(farmId, cow, silentHours);
                }
            }
        }

        return raised;
    }

    /**
     * One animal must not be able to abort the sweep. A cow deleted between the
     * listing and the alert, or any other per-animal failure, is logged and
     * stepped over so the remaining herd is still checked.
     */
    private int raise(Long farmId, Cow cow, long silentHours) {
        try {
            return alertService.createNoSignalAlert(farmId, cow.getCowId(), silentHours) ? 1 : 0;
        } catch (RuntimeException e) {
            log.error("Could not raise no-signal alert for cow {} on farm {}: {}",
                    cow.getCowId(), farmId, e.getMessage());
            return 0;
        }
    }
}
