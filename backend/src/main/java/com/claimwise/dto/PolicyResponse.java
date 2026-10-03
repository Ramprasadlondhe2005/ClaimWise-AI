package com.claimwise.dto;

import com.claimwise.entity.ExpenseCategory;
import com.claimwise.entity.Policy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {

    private Long id;
    private String policyCode;
    private String title;
    private ExpenseCategory category;
    private String description;
    private String ruleText;
    private BigDecimal maximumAmount;
    private boolean receiptRequired;
    private boolean active;

    public static PolicyResponse fromEntity(Policy policy) {
        return PolicyResponse.builder()
                .id(policy.getId())
                .policyCode(policy.getPolicyCode())
                .title(policy.getTitle())
                .category(policy.getCategory())
                .description(policy.getDescription())
                .ruleText(policy.getRuleText())
                .maximumAmount(policy.getMaximumAmount())
                .receiptRequired(policy.isReceiptRequired())
                .active(policy.isActive())
                .build();
    }
}
