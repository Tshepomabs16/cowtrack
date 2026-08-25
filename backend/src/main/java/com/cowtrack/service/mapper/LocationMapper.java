package com.cowtrack.service.mapper;

import com.cowtrack.dto.request.LocationRequest;
import com.cowtrack.dto.response.LocationResponse;
import com.cowtrack.entity.LocationRecord;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {

    public LocationRecord toEntity(LocationRequest request) {
        LocationRecord location = new LocationRecord();
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setAccuracy(request.getAccuracy());
        location.setRecordedAt(java.time.LocalDateTime.now());
        return location;
    }

    public LocationResponse toResponse(LocationRecord location) {
        LocationResponse response = new LocationResponse();
        response.setLocationId(location.getLocationId());
        response.setCowId(location.getCow().getCowId());
        response.setCowName(location.getCow().getName());
        response.setLatitude(location.getLatitude());
        response.setLongitude(location.getLongitude());
        response.setAccuracy(location.getAccuracy());
        response.setRecordedAt(location.getRecordedAt());
        return response;
    }
}
