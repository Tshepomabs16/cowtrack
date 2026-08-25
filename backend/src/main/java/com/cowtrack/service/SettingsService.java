package com.cowtrack.service;

import com.cowtrack.dto.request.PasswordChangeRequest;
import com.cowtrack.dto.request.PreferencesRequest;
import com.cowtrack.dto.request.ProfileUpdateRequest;
import com.cowtrack.dto.response.PreferencesResponse;
import com.cowtrack.dto.response.UserResponse;

import java.util.Map;

/** Profile, password and preference management for the authenticated user. */
public interface SettingsService {

    UserResponse getProfile();

    UserResponse updateProfile(ProfileUpdateRequest request);

    void changePassword(PasswordChangeRequest request);

    PreferencesResponse getPreferences();

    PreferencesResponse updatePreferences(PreferencesRequest request);

    /** The caller's own data, for the settings page's export button. */
    Map<String, Object> exportData();
}
