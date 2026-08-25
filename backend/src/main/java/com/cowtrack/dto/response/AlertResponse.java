package com.cowtrack.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AlertResponse {
    private Long alertId;
    private Long cowId;
    private String cowName;
    private String alertType;
    private String message;
    private Boolean isResolved;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;

    /** Derived from alertType; see AlertServiceImpl#deriveSeverity. */
    private String severity;

    /** Human-readable heading for the alert card, derived from alertType. */
    private String title;
}