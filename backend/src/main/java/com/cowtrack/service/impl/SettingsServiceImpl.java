package com.cowtrack.service.impl;

import com.cowtrack.dto.request.PasswordChangeRequest;
import com.cowtrack.dto.request.PreferencesRequest;
import com.cowtrack.dto.request.ProfileUpdateRequest;
import com.cowtrack.dto.response.PreferencesResponse;
import com.cowtrack.dto.response.UserResponse;
import com.cowtrack.entity.User;
import com.cowtrack.entity.UserPreferences;
import com.cowtrack.exception.BusinessException;
import com.cowtrack.exception.UnauthorizedException;
import com.cowtrack.repository.UserPreferencesRepository;
import com.cowtrack.repository.UserRepository;
import com.cowtrack.service.SettingsService;
import com.cowtrack.service.UserService;
import com.cowtrack.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingsServiceImpl implements SettingsService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile() {
        return userMapper.toResponse(userService.getAuthenticatedUser());
    }

    @Override
    public UserResponse updateProfile(ProfileUpdateRequest request) {
        User user = userService.getAuthenticatedUser();

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        // Only apply the fields the client actually sent.
        applyIfPresent(request.getFullName(), user::setFullName);
        applyIfPresent(request.getPhone(), user::setPhone);
        applyIfPresent(request.getFarmName(), user::setFarmName);
        applyIfPresent(request.getLocation(), user::setLocation);
        applyIfPresent(request.getTimezone(), user::setTimezone);
        applyIfPresent(request.getLanguage(), user::setLanguage);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void changePassword(PasswordChangeRequest request) {
        User user = userService.getAuthenticatedUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("New password must differ from the current one");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences() {
        return toResponse(loadOrDefault(userService.getAuthenticatedUser()));
    }

    @Override
    public PreferencesResponse updatePreferences(PreferencesRequest request) {
        User user = userService.getAuthenticatedUser();
        UserPreferences preferences = preferencesRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> UserPreferences.defaultsFor(user));

        applyIfPresent(request.getTheme(), preferences::setTheme);
        applyIfPresent(request.getLanguage(), preferences::setLanguage);
        applyIfPresent(request.getTimezone(), preferences::setTimezone);
        applyIfPresent(request.getUnits(), preferences::setUnits);
        applyIfPresent(request.getEmailNotifications(), preferences::setEmailNotifications);
        applyIfPresent(request.getPushNotifications(), preferences::setPushNotifications);
        applyIfPresent(request.getAlertNotifications(), preferences::setAlertNotifications);
        applyIfPresent(request.getWeeklyReports(), preferences::setWeeklyReports);

        return toResponse(preferencesRepository.save(preferences));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> exportData() {
        User user = userService.getAuthenticatedUser();

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportedAt", LocalDateTime.now().toString());
        export.put("profile", userMapper.toResponse(user));
        export.put("preferences", toResponse(loadOrDefault(user)));
        return export;
    }

    private UserPreferences loadOrDefault(User user) {
        return preferencesRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> UserPreferences.defaultsFor(user));
    }

    private <T> void applyIfPresent(T value, java.util.function.Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private PreferencesResponse toResponse(UserPreferences preferences) {
        return new PreferencesResponse(
                preferences.getTheme(),
                preferences.getLanguage(),
                preferences.getTimezone(),
                preferences.getUnits(),
                preferences.getEmailNotifications(),
                preferences.getPushNotifications(),
                preferences.getAlertNotifications(),
                preferences.getWeeklyReports()
        );
    }
}
