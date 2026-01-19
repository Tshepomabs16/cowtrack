package com.cowtrack.service;

import com.cowtrack.dto.request.CowRequest;
import com.cowtrack.dto.response.CowResponse;
import com.cowtrack.entity.Cow;

import java.util.List;

public interface CowService {
    CowResponse createCow(CowRequest request);
    CowResponse getCowById(Long cowId);
    CowResponse getCowByTagId(String tagId);
    List<CowResponse> getAllCows();
    List<CowResponse> getCowsByCaretaker(Long caretakerId);
    CowResponse updateCow(Long cowId, CowRequest request);
    void deleteCow(Long cowId);
    Cow getCowEntity(Long cowId);
    List<CowResponse> searchCows(String query);
    CowResponse assignCaretaker(Long cowId, Long caretakerId);
}
