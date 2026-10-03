package com.claimwise.service;

import com.claimwise.dto.AuditLogResponse;
import com.claimwise.dto.CategoryStat;
import com.claimwise.dto.ClaimResponse;
import com.claimwise.dto.DashboardMetricsResponse;
import com.claimwise.entity.ClaimStatus;
import com.claimwise.entity.ExpenseCategory;
import com.claimwise.entity.ExpenseClaim;
import com.claimwise.repository.AuditLogRepository;
import com.claimwise.repository.ExpenseClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ExpenseClaimRepository claimRepository;
    private final AuditLogRepository auditLogRepository;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public DashboardMetricsResponse getDashboardMetrics() {
        List<ExpenseClaim> allClaims = claimRepository.findAllByOrderByCreatedAtDesc();

        long totalClaims = allClaims.size();
        long pendingReviews = allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.SUBMITTED || c.getStatus() == ClaimStatus.UNDER_REVIEW).count();
        long approvedClaims = allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.APPROVED).count();
        long rejectedClaims = allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.REJECTED).count();
        long clarificationRequired = allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.CLARIFICATION_REQUIRED).count();

        BigDecimal totalClaimedAmount = allClaims.stream()
                .map(ExpenseClaim::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal approvedClaimedAmount = allClaims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.APPROVED)
                .map(ExpenseClaim::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Category distribution
        Map<ExpenseCategory, List<ExpenseClaim>> byCategory = allClaims.stream()
                .collect(Collectors.groupingBy(ExpenseClaim::getCategory));

        List<CategoryStat> categoryStats = new ArrayList<>();
        for (ExpenseCategory cat : ExpenseCategory.values()) {
            List<ExpenseClaim> catClaims = byCategory.getOrDefault(cat, Collections.emptyList());
            BigDecimal catTotal = catClaims.stream().map(ExpenseClaim::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            categoryStats.add(CategoryStat.builder()
                    .category(cat)
                    .count(catClaims.size())
                    .totalAmount(catTotal)
                    .build());
        }

        // Status distribution map
        Map<String, Long> statusMap = new HashMap<>();
        for (ClaimStatus s : ClaimStatus.values()) {
            long count = allClaims.stream().filter(c -> c.getStatus() == s).count();
            statusMap.put(s.name(), count);
        }

        // Claims requiring attention (SUBMITTED, UNDER_REVIEW, CLARIFICATION_REQUIRED, or missing receipts)
        List<ClaimResponse> attentionClaims = allClaims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.SUBMITTED || c.getStatus() == ClaimStatus.CLARIFICATION_REQUIRED || !c.isReceiptAvailable())
                .limit(8)
                .map(ClaimResponse::fromEntity)
                .collect(Collectors.toList());

        // Recent audit activity
        List<AuditLogResponse> recentActivity = auditLogRepository.findAll().stream()
                .sorted(Comparator.comparing(com.claimwise.entity.AuditLog::getTimestamp).reversed())
                .limit(6)
                .map(AuditLogResponse::fromEntity)
                .collect(Collectors.toList());

        return DashboardMetricsResponse.builder()
                .totalClaims(totalClaims)
                .pendingReviews(pendingReviews)
                .approvedClaims(approvedClaims)
                .rejectedClaims(rejectedClaims)
                .clarificationRequired(clarificationRequired)
                .totalClaimedAmount(totalClaimedAmount)
                .approvedClaimedAmount(approvedClaimedAmount)
                .categoryDistribution(categoryStats)
                .statusDistribution(statusMap)
                .claimsRequiringAttention(attentionClaims)
                .recentActivity(recentActivity)
                .build();
    }
}
