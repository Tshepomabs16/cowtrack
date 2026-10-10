package com.cowtrack.service.impl;

import com.cowtrack.dto.common.PaginatedResponse;
import com.cowtrack.dto.request.BulkCowUpdateRequest;
import com.cowtrack.dto.request.CowRequest;
import com.cowtrack.dto.response.CowOptionResponse;
import com.cowtrack.dto.response.CowResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Farm;
import com.cowtrack.entity.LocationRecord;
import com.cowtrack.entity.User;
import com.cowtrack.exception.BusinessException;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.exception.ValidationException;
import com.cowtrack.repository.*;
import com.cowtrack.security.FarmContext;
import com.cowtrack.service.CowService;
import com.cowtrack.service.mapper.CowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
    private final LocationRecordRepository locationRecordRepository;
    private final AlertRepository alertRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HealthMetricRepository healthMetricRepository;
    private final ProductionRecordRepository productionRecordRepository;
    private final FarmRepository farmRepository;
    private final CowMapper cowMapper;
    private final FarmContext farmContext;

    @Override
    public CowResponse createCow(CowRequest request) {
        Long farmId = farmContext.getCurrentFarmId();

        if (cowRepository.findByFarmFarmIdAndTagId(farmId, request.getTagId()).isPresent()) {
            throw new BusinessException("Tag ID already exists: " + request.getTagId());
        }

        Cow cow = cowMapper.toEntity(request);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        cow.setFarm(farm);

        if (request.getMotherId() != null) {
            Cow mother = cowRepository.findByFarmFarmIdAndCowId(farmId, request.getMotherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mother cow not found"));
            cow.setMother(mother);
        }

        if (request.getFatherId() != null) {
            Cow father = cowRepository.findByFarmFarmIdAndCowId(farmId, request.getFatherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Father cow not found"));
            cow.setFather(father);
        }

        if (request.getCaretakerId() != null) {
            User caretaker = userRepository.findByFarmFarmIdAndUserId(farmId, request.getCaretakerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caretaker not found"));
            cow.setCaretaker(caretaker);
        }

        Cow savedCow = cowRepository.save(cow);
        return getCowResponse(savedCow);
    }

    @Override
    public CowResponse getCowById(Long cowId) {
        Long farmId = farmContext.getCurrentFarmId();
        Cow cow = cowRepository.findByFarmFarmIdAndCowId(farmId, cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + cowId));
        return getCowResponse(cow);
    }

    @Override
    public CowResponse getCowByTagId(String tagId) {
        Long farmId = farmContext.getCurrentFarmId();
        Cow cow = cowRepository.findByFarmFarmIdAndTagId(farmId, tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with tag: " + tagId));
        return getCowResponse(cow);
    }

    @Override
    public PaginatedResponse<CowResponse> getCows(String search, Pageable pageable) {
        Long farmId = farmContext.getCurrentFarmId();

        // Blank is not a search. Treated as one it would match on '%%', which is
        // harmless, but the intent reads better as "no filter supplied".
        String term = (search == null || search.isBlank()) ? null : search.trim();

        return PaginatedResponse.from(
                cowRepository.findPageForFarm(farmId, term, pageable),
                this::getCowResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CowOptionResponse> getCowOptions() {
        Long farmId = farmContext.getCurrentFarmId();
        return cowRepository.findByFarmFarmId(farmId).stream()
                .map(cow -> new CowOptionResponse(cow.getCowId(), cow.getName(), cow.getTagId()))
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
        Long farmId = farmContext.getCurrentFarmId();
        Cow cow = cowRepository.findByFarmFarmIdAndCowId(farmId, cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + cowId));

        if (!cow.getTagId().equals(request.getTagId())) {
            if (cowRepository.findByFarmFarmIdAndTagId(farmId, request.getTagId()).isPresent()) {
                throw new BusinessException("Tag ID already exists: " + request.getTagId());
            }
        }

        cow.setTagId(request.getTagId());
        cow.setName(request.getName());
        cow.setDateOfBirth(request.getDateOfBirth());
        cow.setBreed(request.getBreed());

        if (request.getMotherId() != null) {
            Cow mother = cowRepository.findByFarmFarmIdAndCowId(farmId, request.getMotherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mother cow not found"));
            cow.setMother(mother);
        } else {
            cow.setMother(null);
        }

        if (request.getFatherId() != null) {
            Cow father = cowRepository.findByFarmFarmIdAndCowId(farmId, request.getFatherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Father cow not found"));
            cow.setFather(father);
        } else {
            cow.setFather(null);
        }

        if (request.getCaretakerId() != null) {
            User caretaker = userRepository.findByFarmFarmIdAndUserId(farmId, request.getCaretakerId())
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
        Long farmId = farmContext.getCurrentFarmId();
        Cow cow = cowRepository.findByFarmFarmIdAndCowId(farmId, cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + cowId));
        cowRepository.delete(cow);
    }

    @Override
    public List<CowResponse> searchCows(String query) {
        Long farmId = farmContext.getCurrentFarmId();
        return cowRepository.findByFarmFarmIdAndNameContainingIgnoreCase(farmId, query).stream()
                .map(this::getCowResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CowResponse assignCaretaker(Long cowId, Long caretakerId) {
        Long farmId = farmContext.getCurrentFarmId();
        Cow cow = cowRepository.findByFarmFarmIdAndCowId(farmId, cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + cowId));
        User caretaker = userRepository.findByFarmFarmIdAndUserId(farmId, caretakerId)
                .orElseThrow(() -> new ResourceNotFoundException("Caretaker not found"));

        cow.setCaretaker(caretaker);
        Cow updatedCow = cowRepository.save(cow);
        return getCowResponse(updatedCow);
    }

    private CowResponse getCowResponse(Cow cow) {
        // Derived fields are read through the owning farm, not just the cow id.
        // The cow is already farm-scoped by every caller, but resolving its
        // related records without a farm predicate would let a record belonging
        // to another farm decide this animal's displayed position or status.
        Long farmId = cow.getFarm() != null
                ? cow.getFarm().getFarmId()
                : farmContext.getCurrentFarmId();

        LocationRecord lastLocation = locationRecordRepository
                .findLatestByFarmIdAndCowId(farmId, cow.getCowId()).orElse(null);
        Integer healthRecordCount = healthRecordRepository.findByCowCowIdOrderByRecordDateDesc(cow.getCowId()).size();
        Boolean hasActiveAlerts = !alertRepository
                .findByFarmFarmIdAndCowCowIdAndIsResolvedFalse(farmId, cow.getCowId()).isEmpty();

        CowResponse response =
                cowMapper.toResponse(cow, lastLocation, healthRecordCount, hasActiveAlerts);

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

        Long farmId = farmContext.getCurrentFarmId();
        List<CowResponse> results = new java.util.ArrayList<>();

        for (BulkCowUpdateRequest update : updates) {
            Cow cow = cowRepository.findByFarmFarmIdAndCowId(farmId, update.getCowId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cow not found with id: " + update.getCowId()));

            if (update.getTagId() != null && !update.getTagId().equals(cow.getTagId())) {
                if (cowRepository.findByFarmFarmIdAndTagId(farmId, update.getTagId()).isPresent()) {
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
                cow.setCaretaker(userRepository.findByFarmFarmIdAndUserId(farmId, update.getCaretakerId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Caretaker not found with id: " + update.getCaretakerId())));
            }

            results.add(getCowResponse(cowRepository.save(cow)));
        }

        return results;
    }
}
