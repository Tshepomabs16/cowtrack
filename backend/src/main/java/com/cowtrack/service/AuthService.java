package com.cowtrack.service;

import com.cowtrack.dto.request.LoginRequest;
import com.cowtrack.dto.request.RegisterRequest;
import com.cowtrack.dto.response.AuthResponse;

public interface AuthService {

    /** Authenticates the credentials and issues a JWT. */
    AuthResponse login(LoginRequest request);

    /** Creates the account and issues a JWT so the client is logged in immediately. */
    AuthResponse register(RegisterRequest request);
}
