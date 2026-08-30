package com.cowtrack.service.impl;

import com.cowtrack.dto.request.DeviceRegistrationRequest;
import com.cowtrack.dto.response.DeviceResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Device;
import com.cowtrack.entity.Farm;
import com.cowtrack.exception.BusinessException;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.DeviceRepository;
import com.cowtrack.repository.FarmRepository;
import com.cowtrack.security.DeviceAuthenticator;
import com.cowtrack.security.FarmContext;
import com.cowtrack.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final CowRepository cowRepository;
    private final FarmRepository farmRepository;
    private final DeviceAuthenticator deviceAuthenticator;
    private final FarmContext farmContext;

    @Override
    public DeviceResponse register(DeviceRegistrationRequest request) {
        Long farmId = farmContext.getCurrentFarmId();

        // Serials are globally unique, so this check is not farm-scoped: a collar
        // already registered to another farm must not be silently re-registered
        // here, which would let one farm capture another's hardware.
        if (deviceRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new BusinessException("A device with serial " + request.getSerialNumber()
                    + " is already registered");
        }

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));

        Device device = new Device();
        device.setSerialNumber(request.getSerialNumber());
        device.setFarm(farm);
        device.setStatus(Device.Status.ACTIVE);

        if (request.getCowId() != null) {
            device.setCow(requireOwnCow(farmId, request.getCowId()));
        }

        String apiKey = deviceAuthenticator.generateKey();
        device.setApiKeyHash(deviceAuthenticator.hash(apiKey));

        Device saved = deviceRepository.save(device);
        log.info("Registered device {} for farm {}", saved.getSerialNumber(), farmId);

        // The only time the key is ever returned.
        DeviceResponse response = toResponse(saved);
        response.setApiKey(apiKey);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> getDevices() {
        return deviceRepository.findByFarmFarmId(farmContext.getCurrentFarmId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DeviceResponse assignToCow(Long deviceId, Long cowId) {
        Long farmId = farmContext.getCurrentFarmId();
        Device device = requireOwnDevice(farmId, deviceId);

        // One collar per animal: two devices reporting for the same cow would
        // interleave positions and make movement look erratic.
        deviceRepository.findByFarmFarmIdAndCowCowId(farmId, cowId)
                .filter(existing -> !existing.getDeviceId().equals(deviceId))
                .ifPresent(existing -> {
                    throw new BusinessException("Cow already has device "
                            + existing.getSerialNumber() + " fitted");
                });

        device.setCow(requireOwnCow(farmId, cowId));
        return toResponse(deviceRepository.save(device));
    }

    @Override
    public DeviceResponse unassign(Long deviceId) {
        Long farmId = farmContext.getCurrentFarmId();
        Device device = requireOwnDevice(farmId, deviceId);
        device.setCow(null);
        return toResponse(deviceRepository.save(device));
    }

    @Override
    public DeviceResponse rotateKey(Long deviceId) {
        Long farmId = farmContext.getCurrentFarmId();
        Device device = requireOwnDevice(farmId, deviceId);

        String apiKey = deviceAuthenticator.generateKey();
        device.setApiKeyHash(deviceAuthenticator.hash(apiKey));
        log.info("Rotated key for device {}", device.getSerialNumber());

        DeviceResponse response = toResponse(deviceRepository.save(device));
        response.setApiKey(apiKey);
        return response;
    }

    @Override
    public DeviceResponse retire(Long deviceId) {
        Long farmId = farmContext.getCurrentFarmId();
        Device device = requireOwnDevice(farmId, deviceId);

        // Retired rather than deleted: the readings it produced are still the
        // animal's history, and deleting the device would orphan them.
        device.setStatus(Device.Status.RETIRED);
        device.setCow(null);
        return toResponse(deviceRepository.save(device));
    }

    private Device requireOwnDevice(Long farmId, Long deviceId) {
        return deviceRepository.findByFarmFarmIdAndDeviceId(farmId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
    }

    private Cow requireOwnCow(Long farmId, Long cowId) {
        return cowRepository.findByFarmFarmIdAndCowId(farmId, cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + cowId));
    }

    private DeviceResponse toResponse(Device device) {
        DeviceResponse response = new DeviceResponse();
        response.setDeviceId(device.getDeviceId());
        response.setSerialNumber(device.getSerialNumber());
        response.setStatus(device.getStatus().name());
        response.setBatteryPercent(device.getBatteryPercent());
        response.setBatteryLow(device.isBatteryLow());
        response.setFirmwareVersion(device.getFirmwareVersion());
        response.setLastSeenAt(device.getLastSeenAt());
        response.setCreatedAt(device.getCreatedAt());

        if (device.getCow() != null) {
            response.setCowId(device.getCow().getCowId());
            response.setCowName(device.getCow().getName());
        }
        return response;
    }
}
