package com.cowtrack.service;

import com.cowtrack.dto.request.VaccinationRequest;
import com.cowtrack.dto.response.VaccinationResponse;

import java.util.List;

public interface VaccinationService {
    VaccinationResponse createVaccination(VaccinationRequest request);
    VaccinationResponse getVaccinationById(Long vaccinationId);
    List<VaccinationResponse> getAllVaccinations();
    List<VaccinationResponse> getVaccinationsByCow(Long cowId);
    List<VaccinationResponse> getPendingVaccinations();
    List<VaccinationResponse> getOverdueVaccinations();
    VaccinationResponse updateVaccination(Long vaccinationId, VaccinationRequest request);
    void deleteVaccination(Long vaccinationId);
}
