package com.cowtrack.controller;

import com.cowtrack.dto.request.DeviceRegistrationRequest;
import com.cowtrack.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Managing collars, for the people who own them. Distinct from
 * {@link IngestController}, which is where the collars themselves report.
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController extends BaseController {

    private final DeviceService deviceService;

    @GetMapping
    public ResponseEntity<?> getDevices() {
        return success(deviceService.getDevices());
    }

    /** Returns the device key in the clear. The only time it is ever exposed. */
    @PostMapping
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> register(@Valid @RequestBody DeviceRegistrationRequest request) {
        return created(deviceService.register(request));
    }

    @PutMapping("/{deviceId}/assign/{cowId}")
    @PreAuthorize("hasRole('FARMER') or hasRole('CARETAKER') or hasRole('ADMIN')")
    public ResponseEntity<?> assign(@PathVariable Long deviceId, @PathVariable Long cowId) {
        return success("Device assigned", deviceService.assignToCow(deviceId, cowId));
    }

    @PutMapping("/{deviceId}/unassign")
    @PreAuthorize("hasRole('FARMER') or hasRole('CARETAKER') or hasRole('ADMIN')")
    public ResponseEntity<?> unassign(@PathVariable Long deviceId) {
        return success("Device unassigned", deviceService.unassign(deviceId));
    }

    @PutMapping("/{deviceId}/rotate-key")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> rotateKey(@PathVariable Long deviceId) {
        return success("New key issued; the previous one no longer works",
                deviceService.rotateKey(deviceId));
    }

    @PutMapping("/{deviceId}/retire")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> retire(@PathVariable Long deviceId) {
        return success("Device retired", deviceService.retire(deviceId));
    }
}
