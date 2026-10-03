package com.claimwise.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false, unique = true)
    private ExpenseClaim claim;

    @Column(nullable = false, length = 50)
    private String classification; // COMPLIANT, NEEDS_CLARIFICATION, NEEDS_REVIEW, NON_COMPLIANT

    @Column(nullable = false)
    private Double confidence; // e.g. 0.85 (85%)

    @Column(nullable = false)
    private boolean uncertain;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(nullable = false, length = 3000)
    private String reasoning;

    @Column(length = 1000)
    private String missingInformation;

    @Column(length = 2000)
    private String policyEvidence; // Cited policy codes & titles

    @Column(nullable = false, length = 50)
    private String recommendedAction; // APPROVE, REJECT, REQUEST_CLARIFICATION, FLAG_FOR_AUDIT

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
