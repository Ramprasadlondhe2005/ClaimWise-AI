package com.claimwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationIssue {
    private String type;     // e.g. MISSING_RECEIPT, CATEGORY_LIMIT_EXCEEDED, DUPLICATE_CLAIM, INVALID_DATE, CURRENCY_UNVERIFIED
    private String severity; // e.g. LOW, MEDIUM, HIGH, CRITICAL
    private String message;
}
