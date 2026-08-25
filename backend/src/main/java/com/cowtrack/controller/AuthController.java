package com.cowtrack.controller;

import com.cowtrack.dto.request.LoginRequest;
import com.cowtrack.dto.request.RegisterRequest;
import com.cowtrack.dto.response.AuthResponse;
import com.cowtrack.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication endpoints consumed by {@code frontend/src/services/api.js}.
 *
 * <p>Response bodies here are intentionally unwrapped (no {@code ApiResponse}
 * envelope) because the web client reads {@code token}, {@code user} and
 * {@code valid} directly off the response body.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Reports whether the caller's bearer token is still good. Reaching this method
     * at all means the filter chain accepted the token, so the answer is yes; an
     * invalid token is rejected earlier with a 401.
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean valid = authentication != null && authentication.isAuthenticated();
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    /**
     * Stateless logout. The server issues no revocation list, so the client simply
     * discards its token; this endpoint exists so the client's logout call succeeds.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out"));
    }
}
