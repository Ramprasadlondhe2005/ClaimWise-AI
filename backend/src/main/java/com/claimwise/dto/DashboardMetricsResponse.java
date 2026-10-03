package com.claimwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsResponse {

    private long totalClaims;
    private long pendingReviews;
    private long approvedClaims;
    private long rejectedClaims;
    private long clarificationRequired;
    private BigDecimal totalClaimedAmount;
    private BigDecimal approvedClaimedAmount;

    private List<CategoryStat> categoryDistribution;
    private Map<String, Long> statusDistribution;
    private List<ClaimResponse> claimsRequiringAttention;
    private List<AuditLogResponse> recentActivity;
}
