package com.cowtrack.controller;

import com.cowtrack.dto.request.CowRequest;
import com.cowtrack.dto.response.CowResponse;
import com.cowtrack.service.CowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cows")
@RequiredArgsConstructor
public class CowController extends BaseController {

    private final CowService cowService;

    @PostMapping
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

    @GetMapping
    public ResponseEntity<?> getAllCows() {
        List<CowResponse> cows = cowService.getAllCows();
        return success(cows);
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
    public ResponseEntity<?> updateCow(
            @PathVariable Long cowId,
            @Valid @RequestBody CowRequest request) {
        CowResponse cow = cowService.updateCow(cowId, request);
        return success("Cow updated successfully", cow);
    }

    @DeleteMapping("/{cowId}")
    public ResponseEntity<?> deleteCow(@PathVariable Long cowId) {
        cowService.deleteCow(cowId);
        return success("Cow deleted successfully", null);
    }

    @PostMapping("/{cowId}/assign-caretaker/{caretakerId}")
    public ResponseEntity<?> assignCaretaker(
            @PathVariable Long cowId,
            @PathVariable Long caretakerId) {
        CowResponse cow = cowService.assignCaretaker(cowId, caretakerId);
        return success("Caretaker assigned successfully", cow);
    }

    @GetMapping("/{cowId}/lineage")
    public ResponseEntity<?> getCowLineage(@PathVariable Long cowId) {
        // This would return mother, father, and children
        // For now, return basic info
        CowResponse cow = cowService.getCowById(cowId);
        return success("Lineage endpoint - implement lineage logic", cow);
    }
}