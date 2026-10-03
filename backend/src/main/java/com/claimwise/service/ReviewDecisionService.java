package com.claimwise.service;

import com.claimwise.dto.*;
import com.claimwise.entity.*;
import com.claimwise.exception.ApiException;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.repository.AiReviewRepository;
import com.claimwise.repository.ExpenseClaimRepository;
import com.claimwise.repository.ReviewDecisionRepository;
import com.claimwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewDecisionService {

    private final ExpenseClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final ReviewDecisionRepository decisionRepository;
    private final AiReviewRepository aiReviewRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ClaimResponse approveClaim(Long claimId, ApproveClaimRequest request, String reviewerEmail) {
        User reviewer = validateReviewer(reviewerEmail);
        ExpenseClaim claim = getClaim(claimId);

        if (claim.getClaimant().getId().equals(reviewer.getId())) {
            throw new ApiException("Employees are strictly forbidden from approving their own expense claims", HttpStatus.FORBIDDEN);
        }

        // Validate status transition
        if (claim.getStatus() == ClaimStatus.APPROVED) {
            throw new ApiException("Claim #" + claimId + " is already APPROVED", HttpStatus.CONFLICT);
        }
        if (claim.getStatus() == ClaimStatus.REJECTED) {
            throw new ApiException("Cannot approve a claim that has already been REJECTED", HttpStatus.CONFLICT);
        }

        ClaimStatus previousStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.APPROVED);
        ExpenseClaim updatedClaim = claimRepository.save(claim);

        ReviewDecision decision = ReviewDecision.builder()
                .claim(updatedClaim)
                .reviewer(reviewer)
                .action("APPROVE")
                .previousStatus(previousStatus)
                .newStatus(ClaimStatus.APPROVED)
                .reason(request.getReason() != null && !request.getReason().trim().isEmpty() ? request.getReason().trim() : "Claim approved by reviewer")
                .build();
        decisionRepository.save(decision);

        auditLogService.recordEvent(
                claimId,
                reviewer,
                "CLAIM_APPROVED",
                "Expense claim #" + claimId + " approved by reviewer " + reviewer.getFullName(),
                "Previous Status: " + previousStatus + " -> APPROVED"
        );

        return ClaimResponse.fromEntity(updatedClaim);
    }

    @Transactional
    public ClaimResponse rejectClaim(Long claimId, RejectClaimRequest request, String reviewerEmail) {
        User reviewer = validateReviewer(reviewerEmail);
        ExpenseClaim claim = getClaim(claimId);

        if (claim.getClaimant().getId().equals(reviewer.getId())) {
            throw new ApiException("Employees cannot process rejection decisions on their own claim", HttpStatus.FORBIDDEN);
        }

        // Validate status transition
        if (claim.getStatus() == ClaimStatus.REJECTED) {
            throw new ApiException("Claim #" + claimId + " is already REJECTED", HttpStatus.CONFLICT);
        }
        if (claim.getStatus() == ClaimStatus.APPROVED) {
            throw new ApiException("Cannot reject a claim that has already been APPROVED", HttpStatus.CONFLICT);
        }

        ClaimStatus previousStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.REJECTED);
        ExpenseClaim updatedClaim = claimRepository.save(claim);

        ReviewDecision decision = ReviewDecision.builder()
                .claim(updatedClaim)
                .reviewer(reviewer)
                .action("REJECT")
                .previousStatus(previousStatus)
                .newStatus(ClaimStatus.REJECTED)
                .reason(request.getReason().trim())
                .build();
        decisionRepository.save(decision);

        auditLogService.recordEvent(
                claimId,
                reviewer,
                "CLAIM_REJECTED",
                "Expense claim #" + claimId + " rejected. Reason: " + request.getReason().trim(),
                "Previous Status: " + previousStatus + " -> REJECTED"
        );

        return ClaimResponse.fromEntity(updatedClaim);
    }

    @Transactional
    public ClaimResponse requestClarification(Long claimId, ClarificationClaimRequest request, String reviewerEmail) {
        User reviewer = validateReviewer(reviewerEmail);
        ExpenseClaim claim = getClaim(claimId);

        if (claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED) {
            throw new ApiException("Cannot request clarification for a claim that has been finalized (" + claim.getStatus() + ")", HttpStatus.CONFLICT);
        }

        ClaimStatus previousStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.CLARIFICATION_REQUIRED);
        ExpenseClaim updatedClaim = claimRepository.save(claim);

        ReviewDecision decision = ReviewDecision.builder()
                .claim(updatedClaim)
                .reviewer(reviewer)
                .action("REQUEST_CLARIFICATION")
                .previousStatus(previousStatus)
                .newStatus(ClaimStatus.CLARIFICATION_REQUIRED)
                .reason(request.getMessage().trim())
                .build();
        decisionRepository.save(decision);

        auditLogService.recordEvent(
                claimId,
                reviewer,
                "CLARIFICATION_REQUESTED",
                "Clarification requested from claimant. Message: " + request.getMessage().trim(),
                "Previous Status: " + previousStatus + " -> CLARIFICATION_REQUIRED"
        );

        return ClaimResponse.fromEntity(updatedClaim);
    }

    @Transactional
    public ReviewDecisionResponse overrideAiClassification(Long claimId, AiOverrideRequest request, String reviewerEmail) {
        User reviewer = validateReviewer(reviewerEmail);
        ExpenseClaim claim = getClaim(claimId);

        Optional<AiReview> aiReviewOpt = aiReviewRepository.findByClaimId(claimId);
        String previousClassification = aiReviewOpt.map(AiReview::getClassification).orElse("UNCLASSIFIED");

        // Update stored AI Review record if present
        if (aiReviewOpt.isPresent()) {
            AiReview aiReview = aiReviewOpt.get();
            aiReview.setClassification(request.getNewClassification().toUpperCase());
            aiReview.setUncertain(false);
            aiReview.setReasoning(aiReview.getReasoning() + "\n[HUMAN REVIEWER OVERRIDE]: " + request.getReason().trim());
            aiReviewRepository.save(aiReview);
        }

        ReviewDecision decision = ReviewDecision.builder()
                .claim(claim)
                .reviewer(reviewer)
                .action("AI_OVERRIDE")
                .previousStatus(claim.getStatus())
                .newStatus(claim.getStatus())
                .reason(request.getReason().trim())
                .previousAIClassification(previousClassification)
                .newClassification(request.getNewClassification().toUpperCase())
                .build();

        ReviewDecision savedDecision = decisionRepository.save(decision);

        auditLogService.recordEvent(
                claimId,
                reviewer,
                "AI_CLASSIFICATION_OVERRIDDEN",
                String.format("AI Classification overridden from '%s' to '%s' by %s. Reason: %s",
                        previousClassification, request.getNewClassification().toUpperCase(), reviewer.getFullName(), request.getReason().trim()),
                "Original: " + previousClassification + " | Overridden: " + request.getNewClassification().toUpperCase()
        );

        return ReviewDecisionResponse.fromEntity(savedDecision);
    }

    public List<ReviewDecisionResponse> getDecisionHistory(Long claimId) {
        return decisionRepository.findByClaimIdOrderByCreatedAtDesc(claimId)
                .stream()
                .map(ReviewDecisionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private User validateReviewer(String reviewerEmail) {
        User user = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", reviewerEmail));

        if (user.getRole() != Role.REVIEWER && user.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied: Only authorized Reviewers or Admins can perform claim decisions", HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private ExpenseClaim getClaim(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense Claim", "id", claimId));
    }
}
