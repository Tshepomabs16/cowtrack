package com.cowtrack.controller;

import com.cowtrack.dto.request.DeviceBatchRequest;
import com.cowtrack.service.DeviceIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Where collars upload.
 *
 * <p>Separate from the rest of the API because the caller is a device, not a
 * person: it authenticates with a long-lived key in headers rather than a JWT,
 * so this path is exempt from the token filter chain and authenticates inside
 * the service instead.
 */
@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
public class IngestController extends BaseController {

    private static final String SERIAL_HEADER = "X-Device-Serial";
    private static final String KEY_HEADER = "X-Device-Key";

    private final DeviceIngestionService ingestionService;

    /**
     * Accepts a batch of buffered readings.
     *
     * <p>Returns 200 with a per-reading breakdown rather than failing the upload
     * when some readings are unusable: the device needs to know the batch landed
     * so it can clear its buffer, and which rows not to send again.
     */
    @PostMapping("/readings")
    public ResponseEntity<?> ingest(
            @RequestHeader(SERIAL_HEADER) String serialNumber,
            @RequestHeader(KEY_HEADER) String apiKey,
            @Valid @RequestBody DeviceBatchRequest batch) {

        return success("Batch received", ingestionService.ingest(serialNumber, apiKey, batch));
    }
}
