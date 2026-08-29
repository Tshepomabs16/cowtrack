package com.cowtrack.service.impl;

import com.cowtrack.dto.request.LoginRequest;
import com.cowtrack.dto.request.RegisterRequest;
import com.cowtrack.dto.response.AuthResponse;
import com.cowtrack.dto.response.UserResponse;
import com.cowtrack.entity.Farm;
import com.cowtrack.entity.User;
import com.cowtrack.exception.BusinessException;
import com.cowtrack.exception.TooManyRequestsException;
import com.cowtrack.exception.UnauthorizedException;
import com.cowtrack.repository.FarmRepository;
import com.cowtrack.repository.UserRepository;
import com.cowtrack.security.JwtService;
import com.cowtrack.security.LoginAttemptService;
import com.cowtrack.service.AuthService;
import com.cowtrack.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String ip = clientIp();

        // A caller that has already fired enough requests at this address is
        // over the line before we spend a BCrypt hash on stopping them.
        if (!loginAttemptService.ipAllowed(ip)) {
            throw new TooManyRequestsException("Too many login attempts. Try again later.");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        boolean credentialsValid = user != null
                && passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!credentialsValid) {
            // Count the failure against the account too; once it has crossed the
            // line the 429 stops the guessing rather than inviting more 401s.
            if (!loginAttemptService.emailAllowed(request.getEmail())) {
                throw new TooManyRequestsException(
                        "Too many failed attempts. This account is temporarily locked.");
            }
            throw new UnauthorizedException("Invalid email or password");
        }

        // The password was right, so the account is not being brute-forced.
        loginAttemptService.recordSuccess(request.getEmail());
        return buildAuthResponse(user);
    }

    /**
     * The address the login request arrived from. Uses the socket address only:
     * honoring the {@code X-Forwarded-For} header would let a caller with no
     * proxy between them and the API forge an arbitrary identity. When this API
     * is deployed behind a reverse proxy that rewrites that header, switch to
     * the first hop of it here.
     */
    private String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return "local";
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

        User savedUser = userRepository.save(user);

        String farmName = request.getFarmName() != null ? request.getFarmName() : "Farm " + savedUser.getUserId();
        Farm farm = new Farm();
        farm.setFarmName(farmName);
        farm.setLocation(request.getPhone());
        Farm savedFarm = farmRepository.save(farm);

        savedUser.getFarms().add(savedFarm);
        userRepository.save(savedUser);

        return buildAuthResponse(savedUser);
    }

    private AuthResponse buildAuthResponse(User user) {
        Long farmId = user.getPrimaryFarmId();
        String token = jwtService.generateToken(
                user.getEmail(), user.getUserId(), user.getRole().name(), farmId);
        UserResponse userResponse = userMapper.toResponse(user);
        return new AuthResponse(token, userResponse);
    }
}
