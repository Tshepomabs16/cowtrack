package com.cowtrack.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Tuning for the collar monitoring sweep, under
 * {@code cowtrack.monitoring.no-signal.*}.
 *
 * <p>The reporting interval a farm expects varies with the hardware: a collar on
 * mains-free solar may report hourly, a low-power tag once a day. The silence
 * threshold therefore has to be configurable rather than assumed.
 */
@Data
@Component
@ConfigurationProperties(prefix = "cowtrack.monitoring.no-signal")
public class CollarMonitoringProperties {

    /** Disable to stop the sweep entirely, e.g. in a test or a read-only replica. */
    private boolean enabled = true;

    /** How long an animal may go unheard before an alert is raised. */
    private Duration threshold = Duration.ofHours(24);

    /**
     * When the sweep runs. Hourly on the hour by default: often enough that a
     * silent collar is noticed within the hour, rarely enough to be cheap.
     */
    private String cron = "0 0 * * * *";

    /**
     * Ignore animals that have never reported a position at all. Such an animal
     * has no collar fitted rather than a failed one, and alerting on it would
     * produce a permanent, unactionable alert for every untagged animal.
     */
    private boolean ignoreNeverReported = true;
}
