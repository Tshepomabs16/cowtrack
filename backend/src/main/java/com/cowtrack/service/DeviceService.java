package com.cowtrack.service;

import com.cowtrack.dto.request.DeviceRegistrationRequest;
import com.cowtrack.dto.response.DeviceResponse;

import java.util.List;

/** Provisioning and lifecycle of tracking collars, for the farm that owns them. */
public interface DeviceService {

    /** Registers a collar and returns its key. The key is not retrievable later. */
    DeviceResponse register(DeviceRegistrationRequest request);

    List<DeviceResponse> getDevices();

    DeviceResponse assignToCow(Long deviceId, Long cowId);

    DeviceResponse unassign(Long deviceId);

    /** Issues a new key, invalidating the old one. For a key believed compromised. */
    DeviceResponse rotateKey(Long deviceId);

    /** Withdraws a collar from service without deleting the readings it produced. */
    DeviceResponse retire(Long deviceId);
}
