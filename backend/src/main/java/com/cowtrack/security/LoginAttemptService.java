package com.cowtrack.security;

import com.cowtrack.config.LoginRateLimitProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window counters for the two brute-force surfaces on the login
 * endpoint: many attempts from one address, and many failed attempts against
 * one account. Both are bounded by a fixed window so a burst does not put the
 * caller into a permanent lockout.
 *
 * <p>State lives in memory and is not shared between instances. That is
 * appropriate for a single-node deployment (the current shape of the project);
 * a multi-instance farm would need to move the counters to Redis or the
 * database, but the {@link LoginAttemptService} API would not change.
 */
@Slf4j
@Component
public class LoginAttemptService {

    private final LoginRateLimitProperties properties;

    /** Client address -> first attempt time + count in the current window. */
    private final Map<String, WindowCounter> ipAttempts = new ConcurrentHashMap<>();

    /** Account (email) -> first failure time + count in the current window. */
    private final Map<String, WindowCounter> emailFailures = new ConcurrentHashMap<>();

    public LoginAttemptService(LoginRateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * Counts a login attempt from {@code ip} in the current window.
     *
     * @return {@code true} while the address is still under its limit
     */
    public boolean ipAllowed(String ip) {
        return !isOverLimit(ipAttempts, ip, properties.getMaxIpAttemptsPerWindow());
    }

    /**
     * Counts a failed login against {@code email} in the current window.
     *
     * @return {@code true} while the account is still under its failure limit
     */
    public boolean emailAllowed(String email) {
        if (!isOverLimit(emailFailures, normalizeEmail(email), properties.getMaxEmailFailuresPerWindow())) {
            return true;
        }
        log.warn("Account locked after repeated failed logins: {}", email);
        return false;
    }

    /**
     * A successful login proves the account holder controls the credentials;
     * wipe the failure streak so occasional typos do not count forever.
     */
    public void recordSuccess(String email) {
        emailFailures.remove(normalizeEmail(email));
    }

    private boolean isOverLimit(Map<String, WindowCounter> counters, String key, int limit) {
        final long now = System.currentTimeMillis();
        WindowCounter latest = counters.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartMillis > properties.getWindow().toMillis()) {
                return new WindowCounter(now, 1);
            }
            return new WindowCounter(existing.windowStartMillis, existing.count + 1);
        });
        return latest.count > limit;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /** Bucket storing the start of the window and how many hits arrived in it. */
    private record WindowCounter(long windowStartMillis, int count) {
    }
}