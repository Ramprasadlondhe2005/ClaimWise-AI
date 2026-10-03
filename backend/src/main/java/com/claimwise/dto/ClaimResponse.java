package com.claimwise.dto;

import com.claimwise.entity.ClaimStatus;
import com.claimwise.entity.ExpenseCategory;
import com.claimwise.entity.ExpenseClaim;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse {

    private Long id;
    private Long claimantId;
    private String claimantName;
    private String claimantEmail;
    private LocalDate expenseDate;
    private ExpenseCategory category;
    private BigDecimal amount;
    private String currency;
    private String description;
    private boolean receiptAvailable;
    private ClaimStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ClaimResponse fromEntity(ExpenseClaim claim) {
        return ClaimResponse.builder()
                .id(claim.getId())
                .claimantId(claim.getClaimant().getId())
                .claimantName(claim.getClaimant().getFullName())
                .claimantEmail(claim.getClaimant().getEmail())
                .expenseDate(claim.getExpenseDate())
                .category(claim.getCategory())
                .amount(claim.getAmount())
                .currency(claim.getCurrency())
                .description(claim.getDescription())
                .receiptAvailable(claim.isReceiptAvailable())
                .status(claim.getStatus())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }
}
