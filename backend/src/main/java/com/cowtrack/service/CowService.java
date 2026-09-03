package com.cowtrack.service;

import com.cowtrack.dto.common.PaginatedResponse;
import com.cowtrack.dto.request.BulkCowUpdateRequest;
import com.cowtrack.dto.request.CowRequest;
import com.cowtrack.dto.response.CowOptionResponse;
import com.cowtrack.dto.response.CowResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CowService {
    CowResponse createCow(CowRequest request);
    CowResponse getCowById(Long cowId);
    CowResponse getCowByTagId(String tagId);

    /**
     * One page of the herd, newest first, optionally narrowed by a search term.
     *
     * <p>Replaces an unpaged listing. Every animal in the response carries
     * derived fields that each cost their own query, so the old version's work
     * grew with the size of the herd twice over.
     */
    PaginatedResponse<CowResponse> getCows(String search, Pageable pageable);

    /** Every animal as an id, name and tag, for pickers. */
    List<CowOptionResponse> getCowOptions();

    List<CowResponse> getCowsByCaretaker(Long caretakerId);
    CowResponse updateCow(Long cowId, CowRequest request);
    void deleteCow(Long cowId);
    List<CowResponse> searchCows(String query);
    CowResponse assignCaretaker(Long cowId, Long caretakerId);

    /** Applies partial updates to many animals in one transaction. */
    List<CowResponse> bulkUpdate(List<BulkCowUpdateRequest> updates);
}
