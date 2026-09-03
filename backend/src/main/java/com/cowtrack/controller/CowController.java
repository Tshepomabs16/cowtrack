package com.cowtrack.controller;

import com.cowtrack.dto.request.BulkCowUpdateRequest;
import com.cowtrack.dto.request.CowRequest;
import com.cowtrack.dto.response.CowResponse;
import com.cowtrack.service.CowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cows")
@RequiredArgsConstructor
public class CowController extends BaseController {

    private final CowService cowService;

    @PostMapping
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> createCow(@Valid @RequestBody CowRequest request) {
        CowResponse cow = cowService.createCow(request);
        return created(cow);
    }

    @GetMapping("/{cowId}")
    public ResponseEntity<?> getCowById(@PathVariable Long cowId) {
        CowResponse cow = cowService.getCowById(cowId);
        return success(cow);
    }

    @GetMapping("/tag/{tagId}")
    public ResponseEntity<?> getCowByTagId(@PathVariable String tagId) {
        CowResponse cow = cowService.getCowByTagId(tagId);
        return success(cow);
    }

    /**
     * One page of the herd.
     *
     * <p>Paged rather than complete. Each animal in the response carries derived
     * fields that cost a query apiece, so an unpaged herd was expensive twice
     * over — in rows returned and in queries run to build them.
     *
     * <p>Callers that need every animal rather than a page of them want
     * {@code /options}, which returns the three fields a picker needs.
     */
    @GetMapping
    public ResponseEntity<?> getCows(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "cowId", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return success(cowService.getCows(search, pageable));
    }

    /** Every animal as id, name and tag, for dropdowns and pickers. */
    @GetMapping("/options")
    public ResponseEntity<?> getCowOptions() {
        return success(cowService.getCowOptions());
    }

    @GetMapping("/caretaker/{caretakerId}")
    public ResponseEntity<?> getCowsByCaretaker(@PathVariable Long caretakerId) {
        List<CowResponse> cows = cowService.getCowsByCaretaker(caretakerId);
        return success(cows);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCows(@RequestParam String query) {
        List<CowResponse> cows = cowService.searchCows(query);
        return success(cows);
    }

    @PutMapping("/{cowId}")
    @PreAuthorize("hasRole('FARMER') or hasRole('CARETAKER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateCow(
            @PathVariable Long cowId,
            @Valid @RequestBody CowRequest request) {
        CowResponse cow = cowService.updateCow(cowId, request);
        return success("Cow updated successfully", cow);
    }

    @DeleteMapping("/{cowId}")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteCow(@PathVariable Long cowId) {
        cowService.deleteCow(cowId);
        return success("Cow deleted successfully", null);
    }

    @PostMapping("/{cowId}/assign-caretaker/{caretakerId}")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> assignCaretaker(
            @PathVariable Long cowId,
            @PathVariable Long caretakerId) {
        CowResponse cow = cowService.assignCaretaker(cowId, caretakerId);
        return success("Caretaker assigned successfully", cow);
    }

    @GetMapping("/{cowId}/lineage")
    public ResponseEntity<?> getCowLineage(@PathVariable Long cowId) {
        CowResponse cow = cowService.getCowById(cowId);
        return success("Lineage endpoint - implement lineage logic", cow);
    }

    @PutMapping("/bulk")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> bulkUpdate(
            @Valid @RequestBody List<BulkCowUpdateRequest> updates) {
        List<CowResponse> cows = cowService.bulkUpdate(updates);
        return success("Updated " + cows.size() + " cows", cows);
    }
}
