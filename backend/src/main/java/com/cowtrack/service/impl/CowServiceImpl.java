package com.cowtrack.service.impl;

import com.cowtrack.dto.request.BulkCowUpdateRequest;
import com.cowtrack.dto.request.CowRequest;
import com.cowtrack.dto.response.CowResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Geofence;
import com.cowtrack.entity.LocationRecord;
import com.cowtrack.entity.User;
import com.cowtrack.exception.BusinessException;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.exception.ValidationException;
import com.cowtrack.repository.*;
import com.cowtrack.service.CowService;
import com.cowtrack.service.mapper.CowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CowServiceImpl implements CowService {

    private final CowRepository cowRepository;
    private final UserRepository userRepository;
    private final GeofenceRepository geofenceRepository;
    private final LocationRecordRepository locationRecordRepository;
    private final AlertRepository alertRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HealthMetricRepository healthMetricRepository;
    private final ProductionRecordRepository productionRecordRepository;
    private final CowMapper cowMapper;

    @Override
    public CowResponse createCow(CowRequest request) {
        // Check if tag ID already exists
        if (cowRepository.existsByTagId(request.getTagId())) {
            throw new BusinessException("Tag ID already exists: " + request.getTagId());
        }

        // Create cow
        Cow cow = cowMapper.toEntity(request);

        // Set mother if provided
        if (request.getMotherId() != null) {
            Cow mother = cowRepository.findById(request.getMotherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mother cow not found"));
            cow.setMother(mother);
        }

        // Set father if provided
        if (request.getFatherId() != null) {
            Cow father = cowRepository.findById(request.getFatherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Father cow not found"));
            cow.setFather(father);
        }

        // Set caretaker if provided
        if (request.getCaretakerId() != null) {
            User caretaker = userRepository.findById(request.getCaretakerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caretaker not found"));
            cow.setCaretaker(caretaker);
        }

        Cow savedCow = cowRepository.save(cow);
        return getCowResponse(savedCow);
    }

    @Override
    public CowResponse getCowById(Long cowId) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + cowId));
        return getCowResponse(cow);
    }

    @Override
    public CowResponse getCowByTagId(String tagId) {
        Cow cow = cowRepository.findByTagId(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with tag: " + tagId));
        return getCowResponse(cow);
    }

    @Override
    public List<CowResponse> getAllCows() {
        return cowRepository.findAll().stream()
                .map(this::getCowResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CowResponse> getCowsByCaretaker(Long caretakerId) {
        return cowRepository.findByCaretakerUserId(caretakerId).stream()
                .map(this::getCowResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CowResponse updateCow(Long cowId, CowRequest request) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + cowId));

        // Check if new tag ID is unique (if changed)
        if (!cow.getTagId().equals(request.getTagId()) && cowRepository.existsByTagId(request.getTagId())) {
            throw new BusinessException("Tag ID already exists: " + request.getTagId());
        }

        cow.setTagId(request.getTagId());
        cow.setName(request.getName());
        cow.setDateOfBirth(request.getDateOfBirth());
        cow.setBreed(request.getBreed());

        // Update relationships if provided
        if (request.getMotherId() != null) {
            Cow mother = cowRepository.findById(request.getMotherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mother cow not found"));
            cow.setMother(mother);
        } else {
            cow.setMother(null);
        }

        if (request.getFatherId() != null) {
            Cow father = cowRepository.findById(request.getFatherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Father cow not found"));
            cow.setFather(father);
        } else {
            cow.setFather(null);
        }

        if (request.getCaretakerId() != null) {
            User caretaker = userRepository.findById(request.getCaretakerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caretaker not found"));
            cow.setCaretaker(caretaker);
        } else {
            cow.setCaretaker(null);
        }

        Cow updatedCow = cowRepository.save(cow);
        return getCowResponse(updatedCow);
    }

    @Override
    public void deleteCow(Long cowId) {
        if (!cowRepository.existsById(cowId)) {
            throw new ResourceNotFoundException("Cow not found with id: " + cowId);
        }
        cowRepository.deleteById(cowId);
    }

    @Override
    public Cow getCowEntity(Long cowId) {
        return cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + cowId));
    }

    @Override
    public List<CowResponse> searchCows(String query) {
        return cowRepository.findByNameContainingIgnoreCase(query).stream()
                .map(this::getCowResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CowResponse assignCaretaker(Long cowId, Long caretakerId) {
        Cow cow = getCowEntity(cowId);
        User caretaker = userRepository.findById(caretakerId)
                .orElseThrow(() -> new ResourceNotFoundException("Caretaker not found"));

        cow.setCaretaker(caretaker);
        Cow updatedCow = cowRepository.save(cow);
        return getCowResponse(updatedCow);
    }

    private CowResponse getCowResponse(Cow cow) {
        // Get geofence if exists
        Geofence geofence = geofenceRepository.findByCowCowId(cow.getCowId()).orElse(null);

        // Get last location
        LocationRecord lastLocation = locationRecordRepository.findLatestByCowId(cow.getCowId()).orElse(null);

        // Count health records
        Integer healthRecordCount = healthRecordRepository.findByCowCowIdOrderByRecordDateDesc(cow.getCowId()).size();

        // Check for active alerts - FIXED METHOD CALL
        Boolean hasActiveAlerts = !alertRepository.findByCowCowIdAndIsResolvedFalse(cow.getCowId()).isEmpty();

        CowResponse response =
                cowMapper.toResponse(cow, geofence, lastLocation, healthRecordCount, hasActiveAlerts);

        if (cow.getDateOfBirth() != null) {
            response.setAge((int) java.time.temporal.ChronoUnit.YEARS.between(
                    cow.getDateOfBirth(), java.time.LocalDate.now()));
        }

        if (lastLocation != null) {
            response.setLocation(String.format("%.4f, %.4f",
                    lastLocation.getLatitude(), lastLocation.getLongitude()));
        }

        healthMetricRepository.findFirstByCow_CowIdOrderByRecordedAtDesc(cow.getCowId())
                .ifPresent(metric -> {
                    response.setTemperature(metric.getTemperature());
                    response.setHeartRate(metric.getHeartRate());
                    response.setLastCheck(metric.getRecordedAt());

                    // Vitals outside their normal band mean the animal needs
                    // attention, and that outranks both "healthy" and a stale
                    // collar: a health problem is more actionable than a missing
                    // GPS fix. Without this the response could report "healthy"
                    // next to a reading the health page marks as a warning.
                    if (metric.isWarning()) {
                        response.setStatus("alert");
                    }
                });

        productionRecordRepository.findByCow_CowIdOrderByRecordDateDesc(cow.getCowId())
                .stream().findFirst()
                .ifPresent(production -> response.setWeight(production.getWeightKg()));

        return response;
    }

    @Override
    public List<CowResponse> bulkUpdate(List<BulkCowUpdateRequest> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new ValidationException("No updates supplied");
        }

        List<CowResponse> results = new java.util.ArrayList<>();

        for (BulkCowUpdateRequest update : updates) {
            Cow cow = cowRepository.findById(update.getCowId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cow not found with id: " + update.getCowId()));

            if (update.getTagId() != null && !update.getTagId().equals(cow.getTagId())) {
                if (cowRepository.existsByTagId(update.getTagId())) {
                    throw new BusinessException("Tag ID already exists: " + update.getTagId());
                }
                cow.setTagId(update.getTagId());
            }
            if (update.getName() != null) {
                cow.setName(update.getName());
            }
            if (update.getBreed() != null) {
                cow.setBreed(update.getBreed());
            }
            if (update.getDateOfBirth() != null) {
                cow.setDateOfBirth(update.getDateOfBirth());
            }
            if (update.getCaretakerId() != null) {
                cow.setCaretaker(userRepository.findById(update.getCaretakerId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Caretaker not found with id: " + update.getCaretakerId())));
            }

            results.add(getCowResponse(cowRepository.save(cow)));
        }

        return results;
    }
}
