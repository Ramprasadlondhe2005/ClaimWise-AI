package com.claimwise.controller;

import com.claimwise.dto.ApiResponse;
import com.claimwise.dto.AiReviewResponse;
import com.claimwise.dto.ClaimResponse;
import com.claimwise.dto.ClaimStatusUpdateRequest;
import com.claimwise.dto.CreateClaimRequest;
import com.claimwise.dto.ValidationResult;
import com.claimwise.entity.ClaimStatus;
import com.claimwise.entity.ExpenseCategory;
import com.claimwise.entity.ExpenseClaim;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.repository.ExpenseClaimRepository;
import com.claimwise.service.AIReviewService;
import com.claimwise.service.DeterministicValidationService;
import com.claimwise.service.ExpenseClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class ExpenseClaimController {

    private final ExpenseClaimService claimService;
    private final DeterministicValidationService validationService;
    private final AIReviewService aiReviewService;
    private final ExpenseClaimRepository claimRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<ClaimResponse>> createClaim(
            @Valid @RequestBody CreateClaimRequest request,
            Authentication authentication
    ) {
        ClaimResponse response = claimService.createClaim(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Expense claim submitted successfully"));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ClaimResponse>>> getMyClaims(Authentication authentication) {
        List<ClaimResponse> claims = claimService.getMyClaims(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(claims, "Fetched employee claims"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClaimResponse>> getClaimById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ClaimResponse response = claimService.getClaimById(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Claim details retrieved"));
    }

    @GetMapping("/{id}/validation")
    public ResponseEntity<ApiResponse<ValidationResult>> getClaimValidation(@PathVariable Long id) {
        ExpenseClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense Claim", "id", id));
        ValidationResult result = validationService.validateClaim(claim);
        return ResponseEntity.ok(ApiResponse.success(result, "Deterministic validation completed"));
    }

    @PostMapping("/{id}/ai-review")
    public ResponseEntity<ApiResponse<AiReviewResponse>> triggerAiReview(@PathVariable Long id) {
        AiReviewResponse response = aiReviewService.generateAiReview(id);
        return ResponseEntity.ok(ApiResponse.success(response, "AI policy review generated successfully"));
    }

    @GetMapping("/{id}/ai-review")
    public ResponseEntity<ApiResponse<AiReviewResponse>> getAiReview(@PathVariable Long id) {
        return aiReviewService.getStoredAiReview(id)
                .map(review -> ResponseEntity.ok(ApiResponse.success(review, "Stored AI review retrieved")))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(null, "No AI review generated yet")));
    }

    @GetMapping("/reviewer/queue")
    public ResponseEntity<ApiResponse<List<ClaimResponse>>> getReviewerQueue(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) ExpenseCategory category
    ) {
        List<ClaimResponse> queue = claimService.getReviewerQueue(status, category);
        return ResponseEntity.ok(ApiResponse.success(queue, "Fetched reviewer claim queue"));
    }

    @PatchMapping("/reviewer/{id}/status")
    public ResponseEntity<ApiResponse<ClaimResponse>> updateClaimStatus(
            @PathVariable Long id,
            @Valid @RequestBody ClaimStatusUpdateRequest request,
            Authentication authentication
    ) {
        ClaimResponse response = claimService.updateClaimStatus(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Claim status updated successfully"));
    }
}
