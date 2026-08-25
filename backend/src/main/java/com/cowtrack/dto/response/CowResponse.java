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
}