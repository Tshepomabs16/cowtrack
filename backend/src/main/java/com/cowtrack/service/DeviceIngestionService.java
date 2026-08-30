package com.cowtrack.service;

import com.cowtrack.dto.request.DeviceBatchRequest;
import com.cowtrack.dto.response.IngestResult;

/**
 * Accepts readings uploaded by tracking collars.
 *
 * <p>This is the path that makes the rest of the system autonomous. Until it
 * existed every position and vital sign had to be POSTed by a human through the
 * authenticated user API, which meant the alerting could only fire in response
 * to somebody typing.
 */
public interface DeviceIngestionService {

    /**
     * Records a batch from an already-authenticated device.
     *
     * @param serialNumber the device's serial, as presented in its credentials
     * @param apiKey       the device's key
     * @param batch        what it has buffered, plus its own state
     */
    IngestResult ingest(String serialNumber, String apiKey, DeviceBatchRequest batch);
}
