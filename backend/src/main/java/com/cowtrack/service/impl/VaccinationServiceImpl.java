package com.cowtrack.service.impl;

import com.cowtrack.dto.request.VaccinationRequest;
import com.cowtrack.dto.response.VaccinationResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Farm;
import com.cowtrack.entity.Vaccination;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.FarmRepository;
import com.cowtrack.repository.VaccinationRepository;
import com.cowtrack.security.FarmContext;
import com.cowtrack.service.VaccinationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VaccinationServiceImpl implements VaccinationService {

    private final VaccinationRepository vaccinationRepository;
    private final CowRepository cowRepository;
    private final FarmRepository farmRepository;
    private final FarmContext farmContext;

    @Override
    public VaccinationResponse createVaccination(VaccinationRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));

        Cow cow = cowRepository.findByFarmFarmIdAndCowId(farmId, request.getCowId())
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + request.getCowId()));

        Vaccination vaccination = new Vaccination();
        vaccination.setFarm(farm);
        vaccination.setCow(cow);
        vaccination.setVaccineName(request.getVaccineName());
        vaccination.setAdministeredDate(request.getAdministeredDate());
        vaccination.setNextDueDate(request.getNextDueDate());
        vaccination.setVetName(request.getVetName());
        vaccination.setNotes(request.getNotes());

        Vaccination saved = vaccinationRepository.save(vaccination);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VaccinationResponse getVaccinationById(Long vaccinationId) {
        Long farmId = farmContext.getCurrentFarmId();
        Vaccination vaccination = vaccinationRepository.findById(vaccinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination not found with id: " + vaccinationId));
        if (vaccination.getFarm() == null || !vaccination.getFarm().getFarmId().equals(farmId)) {
            throw new ResourceNotFoundException("Vaccination not found with id: " + vaccinationId);
        }
        return toResponse(vaccination);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VaccinationResponse> getAllVaccinations() {
        Long farmId = farmContext.getCurrentFarmId();
        return vaccinationRepository.findByFarmFarmIdOrderByAdministeredDateDesc(farmId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VaccinationResponse> getVaccinationsByCow(Long cowId) {
        Long farmId = farmContext.getCurrentFarmId();
        if (!cowRepository.findByFarmFarmIdAndCowId(farmId, cowId).isPresent()) {
            throw new ResourceNotFoundException("Cow not found with id: " + cowId);
        }
        return vaccinationRepository.findByCow_CowIdOrderByAdministeredDateDesc(cowId).stream()
                .filter(v -> v.getFarm() != null && v.getFarm().getFarmId().equals(farmId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VaccinationResponse> getPendingVaccinations() {
        Long farmId = farmContext.getCurrentFarmId();
        return vaccinationRepository.findByFarmFarmIdAndAdministeredDateIsNullOrderByNextDueDateAsc(farmId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VaccinationResponse> getOverdueVaccinations() {
        Long farmId = farmContext.getCurrentFarmId();
        return vaccinationRepository.findByFarmFarmIdAndNextDueDateBeforeOrderByNextDueDateAsc(farmId, LocalDate.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public VaccinationResponse updateVaccination(Long vaccinationId, VaccinationRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Vaccination vaccination = vaccinationRepository.findById(vaccinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination not found with id: " + vaccinationId));
        if (vaccination.getFarm() == null || !vaccination.getFarm().getFarmId().equals(farmId)) {
            throw new ResourceNotFoundException("Vaccination not found with id: " + vaccinationId);
        }

        Cow cow = cowRepository.findByFarmFarmIdAndCowId(farmId, request.getCowId())
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + request.getCowId()));

        vaccination.setCow(cow);
        vaccination.setVaccineName(request.getVaccineName());
        vaccination.setAdministeredDate(request.getAdministeredDate());
        vaccination.setNextDueDate(request.getNextDueDate());
        vaccination.setVetName(request.getVetName());
        vaccination.setNotes(request.getNotes());

        Vaccination updated = vaccinationRepository.save(vaccination);
        return toResponse(updated);
    }

    @Override
    public void deleteVaccination(Long vaccinationId) {
        Long farmId = farmContext.getCurrentFarmId();
        Vaccination vaccination = vaccinationRepository.findById(vaccinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination not found with id: " + vaccinationId));
        if (vaccination.getFarm() == null || !vaccination.getFarm().getFarmId().equals(farmId)) {
            throw new ResourceNotFoundException("Vaccination not found with id: " + vaccinationId);
        }
        vaccinationRepository.deleteById(vaccinationId);
    }

    private VaccinationResponse toResponse(Vaccination v) {
        VaccinationResponse response = new VaccinationResponse();
        response.setVaccinationId(v.getVaccinationId());
        response.setCowId(v.getCow().getCowId());
        response.setCowName(v.getCow().getName());
        response.setVaccineName(v.getVaccineName());
        response.setAdministeredDate(v.getAdministeredDate());
        response.setNextDueDate(v.getNextDueDate());
        response.setVetName(v.getVetName());
        response.setNotes(v.getNotes());
        response.setStatus(v.getStatus().name());
        return response;
    }
}
