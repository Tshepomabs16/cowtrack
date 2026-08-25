package com.cowtrack.controller;

import com.cowtrack.dto.request.PasswordChangeRequest;
import com.cowtrack.dto.request.PreferencesRequest;
import com.cowtrack.dto.request.ProfileUpdateRequest;
import com.cowtrack.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Settings for the authenticated user. No endpoint here takes a user id: the
 *  caller can only ever read or change their own account. */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController extends BaseController {

    private final SettingsService settingsService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        return success(settingsService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return success("Profile updated successfully", settingsService.updateProfile(request));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        settingsService.changePassword(request);
        return success("Password changed successfully", null);
    }

    @GetMapping("/preferences")
    public ResponseEntity<?> getPreferences() {
        return success(settingsService.getPreferences());
    }

    @PutMapping("/preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody PreferencesRequest request) {
        return success("Preferences updated successfully",
                settingsService.updatePreferences(request));
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportData() {
        return success(settingsService.exportData());
    }
}
