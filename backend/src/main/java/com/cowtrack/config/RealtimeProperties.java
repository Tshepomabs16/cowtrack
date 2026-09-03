package com.cowtrack.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Tuning for the live push channel, under {@code cowtrack.realtime.*}.
 */
@Data
@Component
@ConfigurationProperties(prefix = "cowtrack.realtime")
public class RealtimeProperties {

    private boolean enabled = true;

    /**
     * How long a stream is held open before the server closes it and the client
     * reconnects.
     *
     * <p>Not unlimited on purpose. An emitter whose client vanished without a
     * TCP close is only discovered when a write to it fails, so on a quiet farm
     * a dead subscriber could sit in the registry indefinitely. Recycling the
     * connection puts a ceiling on that.
     */
    private Duration streamTimeout = Duration.ofMinutes(30);

    /**
     * Gap between keep-alive comments.
     *
     * <p>Cattle positions can be many minutes apart, and an idle connection is
     * exactly what proxies and mobile networks drop. The comment is also how a
     * subscriber that has gone away is detected, since the write fails.
     */
    private Duration heartbeatInterval = Duration.ofSeconds(25);

    /**
     * How long a stream ticket stays redeemable.
     *
     * <p>Short by design: the ticket travels in the query string, where the JWT
     * deliberately does not, so it ends up in access logs. Seconds of validity
     * and a single use keep that exposure bounded.
     */
    private Duration ticketTtl = Duration.ofSeconds(60);
}
