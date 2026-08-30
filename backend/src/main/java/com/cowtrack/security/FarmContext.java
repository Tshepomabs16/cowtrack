package com.cowtrack.security;

import com.cowtrack.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class FarmContext {

    /**
     * Farm to act as when there is no JWT to read one from.
     *
     * <p>Work not driven by a logged-in user still has an owning farm: a collar
     * uploading readings, a scheduled sweep. Each such caller previously had to
     * be served by a parallel farm-explicit overload of every service method it
     * touched, which multiplies the API once per caller. Setting the farm here
     * instead lets them reuse the ordinary methods.
     *
     * <p>Confined to the calling thread and always restored, so it cannot leak
     * into the next request served by the same pooled thread.
     */
    private static final ThreadLocal<Long> ACTING_FARM = new ThreadLocal<>();

    private final JwtService jwtService;

    /**
     * Runs {@code work} as though the given farm were the authenticated one.
     *
     * <p>Only for callers with no request context that have independently
     * established which farm they act for — a device that authenticated by key,
     * or a sweep iterating farms. Never pass a farm id taken from user input:
     * this bypasses the check that the caller may act for that farm.
     */
    public <T> T actingAsFarm(Long farmId, Supplier<T> work) {
        if (farmId == null) {
            throw new UnauthorizedException("No farm supplied to act as");
        }
        Long previous = ACTING_FARM.get();
        ACTING_FARM.set(farmId);
        try {
            return work.get();
        } finally {
            // Restored rather than cleared, so a nested scope cannot silently
            // drop the one that enclosed it.
            if (previous == null) {
                ACTING_FARM.remove();
            } else {
                ACTING_FARM.set(previous);
            }
        }
    }

    public void actingAsFarm(Long farmId, Runnable work) {
        actingAsFarm(farmId, () -> {
            work.run();
            return null;
        });
    }

    /**
     * The farm the current work belongs to: an explicitly set acting farm when
     * one is in scope, otherwise the farm carried in the caller's JWT.
     */
    public Long getCurrentFarmId() {
        Long acting = ACTING_FARM.get();
        if (acting != null) {
            return acting;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getCredentials() == null) {
            throw new UnauthorizedException("No authenticated user");
        }

        String token = auth.getCredentials().toString();
        Long farmId = jwtService.extractFarmId(token);
        if (farmId == null) {
            throw new UnauthorizedException("No farm associated with this user");
        }
        return farmId;
    }

    /**
     * Returns the farm ID from the JWT token, or null if not present.
     */
    public Long getCurrentFarmIdOrNull() {
        Long acting = ACTING_FARM.get();
        if (acting != null) {
            return acting;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getCredentials() == null) {
            return null;
        }

        String token = auth.getCredentials().toString();
        return jwtService.extractFarmId(token);
    }

    /**
     * Extracts the user ID from the current user's JWT token.
     */
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getCredentials() == null) {
            throw new UnauthorizedException("No authenticated user");
        }

        String token = auth.getCredentials().toString();
        return jwtService.extractUserId(token);
    }

    /**
     * Extracts the user's role from the current user's JWT token.
     */
    public String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getCredentials() == null) {
            throw new UnauthorizedException("No authenticated user");
        }

        String token = auth.getCredentials().toString();
        return jwtService.extractRole(token);
    }
}
