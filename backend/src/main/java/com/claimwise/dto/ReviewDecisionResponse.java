package com.claimwise.dto;

import com.claimwise.entity.ClaimStatus;
import com.claimwise.entity.ReviewDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecisionResponse {

    private Long id;
    private Long claimId;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerEmail;
    private String action;
    private ClaimStatus previousStatus;
    private ClaimStatus newStatus;
    private String reason;
    private String previousAIClassification;
    private String newClassification;
    private LocalDateTime createdAt;

    public static ReviewDecisionResponse fromEntity(ReviewDecision decision) {
        return ReviewDecisionResponse.builder()
                .id(decision.getId())
                .claimId(decision.getClaim().getId())
                .reviewerId(decision.getReviewer().getId())
                .reviewerName(decision.getReviewer().getFullName())
                .reviewerEmail(decision.getReviewer().getEmail())
                .action(decision.getAction())
                .previousStatus(decision.getPreviousStatus())
                .newStatus(decision.getNewStatus())
                .reason(decision.getReason())
                .previousAIClassification(decision.getPreviousAIClassification())
                .newClassification(decision.getNewClassification())
                .createdAt(decision.getCreatedAt())
                .build();
    }
}
