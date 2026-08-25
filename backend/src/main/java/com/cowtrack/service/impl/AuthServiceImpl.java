package com.cowtrack.service.impl;

import com.cowtrack.dto.request.LoginRequest;
import com.cowtrack.dto.request.RegisterRequest;
import com.cowtrack.dto.response.AuthResponse;
import com.cowtrack.dto.response.UserResponse;
import com.cowtrack.entity.User;
import com.cowtrack.exception.BusinessException;
import com.cowtrack.exception.UnauthorizedException;
import com.cowtrack.repository.UserRepository;
import com.cowtrack.security.JwtService;
import com.cowtrack.service.AuthService;
import com.cowtrack.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
        user.setPhone(request.getPhone());
        user.setFarmName(request.getFarmName());
        user.setCreatedAt(LocalDateTime.now());

        return buildAuthResponse(userRepository.save(user));
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(
                user.getEmail(), user.getUserId(), user.getRole().name());
        UserResponse userResponse = userMapper.toResponse(user);
        return new AuthResponse(token, userResponse);
    }
}
