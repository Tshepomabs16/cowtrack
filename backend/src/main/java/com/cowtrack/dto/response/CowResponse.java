package com.cowtrack.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CowResponse {
    private Long cowId;
    private String tagId;
    private String name;
    private LocalDate dateOfBirth;
    private String breed;
    private Long motherId;
    private String motherName;
    private Long fatherId;
    private String fatherName;
    private Long caretakerId;
    private String caretakerName;
    private LocalDateTime createdAt;
    private GeofenceResponse geofence;
    private LocationResponse lastLocation;
    private Integer healthRecordCount;
    private Boolean hasActiveAlerts;

    /** Derived, not persisted. See CowMapper#deriveStatus. */
    private String status;

    /** Age in years, from dateOfBirth. */
    private Integer age;

    /** Latest collar vitals, when the animal has reported any. */
    private java.math.BigDecimal temperature;
    private java.math.BigDecimal heartRate;

    /** Most recent recorded weight in kilograms. */
    private java.math.BigDecimal weight;

    /** When vitals were last received. */
    private java.time.LocalDateTime lastCheck;

    /** Human-readable last known position. */
    private String location;
}