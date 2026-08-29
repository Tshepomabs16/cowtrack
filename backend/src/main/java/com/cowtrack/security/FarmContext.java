package com.cowtrack.security;

import com.cowtrack.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FarmContext {

    private final JwtService jwtService;

    /**
     * Extracts the farm ID from the current user's JWT token.
     * Throws UnauthorizedException if no valid token is present.
     */
    public Long getCurrentFarmId() {
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
