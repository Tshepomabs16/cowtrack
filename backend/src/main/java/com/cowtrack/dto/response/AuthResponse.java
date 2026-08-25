package com.cowtrack.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response body for login and register.
 *
 * <p>Deliberately <em>not</em> wrapped in {@link com.cowtrack.dto.common.ApiResponse}:
 * the web client destructures {@code { token, user }} straight off the response body
 * in {@code AuthContext.jsx}. Wrapping it would nest the payload one level deeper and
 * break the client.
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UserResponse user;
}
