package com.claimwise.service;

import com.claimwise.dto.ClaimResponse;
import com.claimwise.dto.ClaimStatusUpdateRequest;
import com.claimwise.dto.CreateClaimRequest;
import com.claimwise.dto.ValidationResult;
import com.claimwise.entity.*;
import com.claimwise.exception.ApiException;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.repository.ExpenseClaimRepository;
import com.claimwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseClaimService {

    private final ExpenseClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final DeterministicValidationService validationService;

    @Transactional
    public ClaimResponse createClaim(CreateClaimRequest request, String userEmail) {
        User claimant = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (request.getExpenseDate().isAfter(LocalDate.now())) {
            throw new ApiException("Expense date cannot be in the future", HttpStatus.BAD_REQUEST);
        }

        ExpenseClaim claim = ExpenseClaim.builder()
                .claimant(claimant)
                .expenseDate(request.getExpenseDate())
                .category(request.getCategory())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .description(request.getDescription().trim())
                .receiptAvailable(request.isReceiptAvailable())
                .status(ClaimStatus.SUBMITTED) // Deterministic initial status
                .build();

        ExpenseClaim savedClaim = claimRepository.save(claim);

        // Audit Logging
        auditLogService.recordEvent(
                savedClaim.getId(),
                claimant,
                "CLAIM_CREATED",
                String.format("Expense claim submitted for %s %s in category %s",
                        savedClaim.getAmount(), savedClaim.getCurrency(), savedClaim.getCategory()),
                "Receipt Available: " + savedClaim.isReceiptAvailable()
        );

        // Run deterministic validation & record audit
        ValidationResult valResult = validationService.validateClaim(savedClaim);
        auditLogService.recordSystemEvent(
                savedClaim.getId(),
                "VALIDATION_COMPLETED",
                "Deterministic rule validation outcome: " + valResult.getStatus(),
                "Issues Count: " + valResult.getIssues().size()
        );

        return ClaimResponse.fromEntity(savedClaim);
    }

    public List<ClaimResponse> getMyClaims(String userEmail) {
        User claimant = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return claimRepository.findByClaimantIdOrderByCreatedAtDesc(claimant.getId())
                .stream()
                .map(ClaimResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ClaimResponse getClaimById(Long claimId, String userEmail) {
        ExpenseClaim claim = claimRepository.findByIdWithClaimant(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense Claim", "id", claimId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        // Employee can only view their own claim, Reviewer/Admin can view any claim
        if (user.getRole() == Role.EMPLOYEE && !claim.getClaimant().getId().equals(user.getId())) {
            throw new ApiException("Unauthorized access to expense claim", HttpStatus.FORBIDDEN);
        }

        return ClaimResponse.fromEntity(claim);
    }

    public List<ClaimResponse> getReviewerQueue(ClaimStatus status, ExpenseCategory category) {
        List<ExpenseClaim> claims;

        if (status != null && category != null) {
            claims = claimRepository.findByStatusOrderByCreatedAtDesc(status)
                    .stream()
                    .filter(c -> c.getCategory() == category)
                    .collect(Collectors.toList());
        } else if (status != null) {
            claims = claimRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (category != null) {
            claims = claimRepository.findByCategoryOrderByCreatedAtDesc(category);
        } else {
            claims = claimRepository.findAllByOrderByCreatedAtDesc();
        }

        return claims.stream()
                .map(ClaimResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClaimResponse updateClaimStatus(Long claimId, ClaimStatusUpdateRequest request, String reviewerEmail) {
        ExpenseClaim claim = claimRepository.findByIdWithClaimant(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense Claim", "id", claimId));

        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", reviewerEmail));

        if (reviewer.getRole() == Role.EMPLOYEE) {
            throw new ApiException("Employees cannot modify claim status directly", HttpStatus.FORBIDDEN);
        }

        claim.setStatus(request.getStatus());
        ExpenseClaim updated = claimRepository.save(claim);
        return ClaimResponse.fromEntity(updated);
    }
}
