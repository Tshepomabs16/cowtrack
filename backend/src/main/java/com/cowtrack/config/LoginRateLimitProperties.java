package com.cowtrack.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Tuning knobs for the login rate limiter. Overridable through the
 * {@code cowtrack.security.login-rate-limit.*} properties or environment
 * variables, so a deployment can tighten the window without a code change.
 */
@Data
@Component
@ConfigurationProperties(prefix = "cowtrack.security.login-rate-limit")
public class LoginRateLimitProperties {

    /** Max login requests from a single client address per window. */
    private int maxIpAttemptsPerWindow = 30;

    /** Failed logins against one account per window before it locks. */
    private int maxEmailFailuresPerWindow = 5;

    /** The window in which the two limits above apply. */
    private Duration window = Duration.ofMinutes(15);
}