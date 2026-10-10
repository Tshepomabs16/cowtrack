package com.cowtrack.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * What a delete actually did. A fence that has raised alerts is retired rather
 * than removed, so the alerts keep pointing at the boundary that raised them.
 */
@Data
@AllArgsConstructor
public class GeofenceDeletionResult {
    private Long geofenceId;
    private boolean deleted;
    private boolean retired;
    private String reason;
}
