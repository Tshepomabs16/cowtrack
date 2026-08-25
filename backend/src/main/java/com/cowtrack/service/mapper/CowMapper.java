package com.cowtrack.service.mapper;

import com.cowtrack.dto.request.CowRequest;
import com.cowtrack.dto.response.CowResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Geofence;
import com.cowtrack.entity.LocationRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CowMapper {

    public Cow toEntity(CowRequest request) {
        Cow cow = new Cow();
        cow.setTagId(request.getTagId());
        cow.setName(request.getName());
        cow.setDateOfBirth(request.getDateOfBirth());
        cow.setBreed(request.getBreed());
        cow.setCreatedAt(java.time.LocalDateTime.now());
        // Parent relationships will be set in service
        return cow;
    }

    public CowResponse toResponse(Cow cow, Geofence geofence, LocationRecord lastLocation,
                                  Integer healthRecordCount, Boolean hasActiveAlerts) {
        CowResponse response = new CowResponse();
        response.setCowId(cow.getCowId());
        response.setTagId(cow.getTagId());
        response.setName(cow.getName());
        response.setDateOfBirth(cow.getDateOfBirth());
        response.setBreed(cow.getBreed());
        response.setCreatedAt(cow.getCreatedAt());
        response.setStatus(deriveStatus(lastLocation, hasActiveAlerts));

        // Set parent info if available
        if (cow.getMother() != null) {
            response.setMotherId(cow.getMother().getCowId());
            response.setMotherName(cow.getMother().getName());
        }

        if (cow.getFather() != null) {
            response.setFatherId(cow.getFather().getCowId());
            response.setFatherName(cow.getFather().getName());
        }

        // Set caretaker info if available
        if (cow.getCaretaker() != null) {
            response.setCaretakerId(cow.getCaretaker().getUserId());
            response.setCaretakerName(cow.getCaretaker().getFullName());
        }

        // Set additional info
        response.setHealthRecordCount(healthRecordCount);
        response.setHasActiveAlerts(hasActiveAlerts);

        return response;
    }

    /**
     * Derives the status the cattle list filters on. The domain has no status column,
     * so it is inferred from signals that do exist: an unresolved alert, or a collar
     * that has not reported recently.
     */
    private String deriveStatus(LocationRecord lastLocation, Boolean hasActiveAlerts) {
        if (Boolean.TRUE.equals(hasActiveAlerts)) {
            return "alert";
        }
        if (lastLocation == null
                || lastLocation.getRecordedAt() == null
                || lastLocation.getRecordedAt().isBefore(LocalDateTime.now().minusHours(24))) {
            return "inactive";
        }
        return "healthy";
    }
}