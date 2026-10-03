package com.claimwise.controller;

import com.claimwise.dto.*;
import com.claimwise.service.AuditLogService;
import com.claimwise.service.ReviewDecisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/claims/{claimId}")
@RequiredArgsConstructor
public class ReviewDecisionController {

    private final ReviewDecisionService decisionService;
    private final AuditLogService auditLogService;

    @PostMapping("/review/approve")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ClaimResponse>> approveClaim(
            @PathVariable Long claimId,
            @RequestBody(required = false) ApproveClaimRequest request,
            Authentication authentication
    ) {
        if (request == null) {
            request = new ApproveClaimRequest();
        }
        ClaimResponse response = decisionService.approveClaim(claimId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Expense claim approved successfully"));
    }

    @PostMapping("/review/reject")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ClaimResponse>> rejectClaim(
            @PathVariable Long claimId,
            @Valid @RequestBody RejectClaimRequest request,
            Authentication authentication
    ) {
        ClaimResponse response = decisionService.rejectClaim(claimId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Expense claim rejected successfully"));
    }

    @PostMapping("/review/clarification")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ClaimResponse>> requestClarification(
            @PathVariable Long claimId,
            @Valid @RequestBody ClarificationClaimRequest request,
            Authentication authentication
    ) {
        ClaimResponse response = decisionService.requestClarification(claimId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Clarification requested from claimant"));
    }

    @PostMapping("/review/override-ai")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReviewDecisionResponse>> overrideAiClassification(
            @PathVariable Long claimId,
            @Valid @RequestBody AiOverrideRequest request,
            Authentication authentication
    ) {
        ReviewDecisionResponse response = decisionService.overrideAiClassification(claimId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "AI classification overridden successfully"));
    }

    @GetMapping("/decisions")
    public ResponseEntity<ApiResponse<List<ReviewDecisionResponse>>> getDecisionHistory(@PathVariable Long claimId) {
        List<ReviewDecisionResponse> history = decisionService.getDecisionHistory(claimId);
        return ResponseEntity.ok(ApiResponse.success(history, "Decision history retrieved"));
    }

    @GetMapping("/audit-trail")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditTrail(@PathVariable Long claimId) {
        List<AuditLogResponse> logs = auditLogService.getClaimAuditTrail(claimId);
        return ResponseEntity.ok(ApiResponse.success(logs, "Complete audit trail retrieved"));
    }
}
