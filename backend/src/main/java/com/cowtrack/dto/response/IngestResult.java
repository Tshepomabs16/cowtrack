package com.cowtrack.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * What happened to each reading in a batch.
 *
 * <p>Reported per reading rather than as a single pass/fail because a collar
 * cannot fix a bad sample: rejecting the whole upload for one malformed row
 * would lose the good ones and the device would resend the same bad row
 * forever. The device needs to know the batch was received so it can clear its
 * buffer, and which rows to stop sending.
 */
@Data
public class IngestResult {

    private int accepted;

    /** Already stored. A retry after a lost acknowledgement, not an error. */
    private int duplicates;

    private int rejected;

    /** Why each rejected reading was refused, by position in the batch. */
    private List<RejectedReading> errors = new ArrayList<>();

    /**
     * True when the batch moved the animal's newest position forward and alert
     * evaluation therefore ran. A backfill of older readings does not.
     */
    private boolean alertsEvaluated;

    /** Echoed so a device can confirm the server saw the charge it reported. */
    private Integer batteryPercent;

    private LocalDateTime receivedAt = LocalDateTime.now();

    public void reject(int index, String reason) {
        rejected++;
        errors.add(new RejectedReading(index, reason));
    }

    @Data
    public static class RejectedReading {
        private final int index;
        private final String reason;
    }
}
