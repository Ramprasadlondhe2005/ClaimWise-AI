package com.claimwise.dto;

import com.claimwise.entity.AiReview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewResponse {

    private Long id;
    private Long claimId;
    private String classification; // COMPLIANT, NEEDS_CLARIFICATION, NEEDS_REVIEW, NON_COMPLIANT
    private Double confidence;
    private boolean uncertain;
    private String summary;
    private String reasoning;
    private String missingInformation;
    private String policyEvidence;
    private String recommendedAction;
    private LocalDateTime createdAt;
    private String disclaimer;

    public static AiReviewResponse fromEntity(AiReview review) {
        return AiReviewResponse.builder()
                .id(review.getId())
                .claimId(review.getClaim().getId())
                .classification(review.getClassification())
                .confidence(review.getConfidence())
                .uncertain(review.isUncertain())
                .summary(review.getSummary())
                .reasoning(review.getReasoning())
                .missingInformation(review.getMissingInformation())
                .policyEvidence(review.getPolicyEvidence())
                .recommendedAction(review.getRecommendedAction())
                .createdAt(review.getCreatedAt())
                .disclaimer("AI analysis is advisory. Final claim decisions are made by an authorized reviewer.")
                .build();
    }
}
