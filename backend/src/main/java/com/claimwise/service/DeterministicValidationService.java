package com.claimwise.service;

import com.claimwise.dto.ValidationIssue;
import com.claimwise.dto.ValidationResult;
import com.claimwise.entity.ExpenseClaim;
import com.claimwise.entity.Policy;
import com.claimwise.repository.ExpenseClaimRepository;
import com.claimwise.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeterministicValidationService {

    private final PolicyRepository policyRepository;
    private final ExpenseClaimRepository claimRepository;

    public ValidationResult validateClaim(ExpenseClaim claim) {
        List<ValidationIssue> issues = new ArrayList<>();

        // 1. Required Fields & Value Check
        if (claim.getAmount() == null || claim.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            issues.add(ValidationIssue.builder()
                    .type("INVALID_AMOUNT")
                    .severity("CRITICAL")
                    .message("Expense claim amount must be greater than 0.")
                    .build());
        }

        if (claim.getDescription() == null || claim.getDescription().trim().length() < 5) {
            issues.add(ValidationIssue.builder()
                    .type("INSUFFICIENT_DESCRIPTION")
                    .severity("HIGH")
                    .message("Expense description is too short or blank. A detailed business justification is required.")
                    .build());
        }

        // 2. Date Validation
        if (claim.getExpenseDate() != null) {
            if (claim.getExpenseDate().isAfter(LocalDate.now())) {
                issues.add(ValidationIssue.builder()
                        .type("FUTURE_DATE")
                        .severity("CRITICAL")
                        .message("Expense date cannot be in the future.")
                        .build());
            } else if (claim.getExpenseDate().isBefore(LocalDate.now().minusDays(90))) {
                issues.add(ValidationIssue.builder()
                        .type("STALE_EXPENSE_DATE")
                        .severity("LOW")
                        .message("Expense date is older than 90 days. Special manager override may be required.")
                        .build());
            }
        }

        // 3. Category Spending Limits & Missing Receipt Checks against Policy Repository
        List<Policy> categoryPolicies = policyRepository.findByCategoryAndActiveTrue(claim.getCategory());
        if (!categoryPolicies.isEmpty()) {
            Policy categoryPolicy = categoryPolicies.get(0);

            // Maximum amount limit check
            if (claim.getAmount() != null && categoryPolicy.getMaximumAmount() != null) {
                if (claim.getAmount().compareTo(categoryPolicy.getMaximumAmount()) > 0) {
                    issues.add(ValidationIssue.builder()
                            .type("CATEGORY_LIMIT_EXCEEDED")
                            .severity("HIGH")
                            .message(String.format("Claim amount (%s %s) exceeds the maximum allowed limit (%s %s) for category %s under policy rule [%s].",
                                    claim.getAmount(), claim.getCurrency(), categoryPolicy.getMaximumAmount(), claim.getCurrency(), claim.getCategory(), categoryPolicy.getPolicyCode()))
                            .build());
                }
            }

            // Missing Receipt Check
            if (categoryPolicy.isReceiptRequired() && !claim.isReceiptAvailable()) {
                issues.add(ValidationIssue.builder()
                        .type("MISSING_RECEIPT")
                        .severity("HIGH")
                        .message(String.format("Policy rule [%s] mandates a receipt for %s expenses, but no receipt was attached.",
                                categoryPolicy.getPolicyCode(), claim.getCategory()))
                        .build());
            }
        } else {
            // Default rule fallback if no specific policy row exists
            if (!claim.isReceiptAvailable() && claim.getAmount() != null && claim.getAmount().compareTo(new BigDecimal("50.00")) > 0) {
                issues.add(ValidationIssue.builder()
                        .type("MISSING_RECEIPT")
                        .severity("HIGH")
                        .message("Standard organizational policy requires receipts for all expenses exceeding 50.00.")
                        .build());
            }
        }

        // 4. Duplicate Claim Detection
        if (claim.getClaimant() != null && claim.getExpenseDate() != null && claim.getAmount() != null) {
            List<ExpenseClaim> userClaims = claimRepository.findByClaimantIdOrderByCreatedAtDesc(claim.getClaimant().getId());
            for (ExpenseClaim existing : userClaims) {
                // Ignore self if editing/validating existing saved record
                if (claim.getId() != null && claim.getId().equals(existing.getId())) {
                    continue;
                }

                boolean sameDate = claim.getExpenseDate().equals(existing.getExpenseDate());
                boolean sameAmount = claim.getAmount().compareTo(existing.getAmount()) == 0;
                boolean sameCategory = claim.getCategory() == existing.getCategory();

                if (sameDate && (sameAmount || sameCategory)) {
                    issues.add(ValidationIssue.builder()
                            .type("DUPLICATE_CLAIM")
                            .severity("CRITICAL")
                            .message(String.format("Potential duplicate claim detected! Claim #%d was previously submitted on %s for %s %s (%s).",
                                    existing.getId(), existing.getExpenseDate(), existing.getAmount(), existing.getCurrency(), existing.getCategory()))
                            .build());
                    break;
                }
            }
        }

        // 5. Currency Verification
        List<String> standardCurrencies = List.of("USD", "EUR", "GBP", "INR", "CAD", "AUD");
        if (claim.getCurrency() != null && !standardCurrencies.contains(claim.getCurrency().toUpperCase())) {
            issues.add(ValidationIssue.builder()
                    .type("CURRENCY_VERIFICATION")
                    .severity("LOW")
                    .message("Non-standard currency specified. FX exchange rate audit required.")
                    .build());
        }

        // Determine overall status
        boolean hasCriticalOrHigh = issues.stream().anyMatch(i -> "CRITICAL".equalsIgnoreCase(i.getSeverity()) || "HIGH".equalsIgnoreCase(i.getSeverity()));
        boolean hasAnyIssue = !issues.isEmpty();

        String status = "PASS";
        if (hasCriticalOrHigh) {
            status = "FAILED";
        } else if (hasAnyIssue) {
            status = "WARNING";
        }

        return ValidationResult.builder()
                .passed("PASS".equals(status))
                .status(status)
                .issues(issues)
                .build();
    }
}
