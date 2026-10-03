package com.claimwise.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_decisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private ExpenseClaim claim;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Column(nullable = false, length = 50)
    private String action; // APPROVE, REJECT, REQUEST_CLARIFICATION, AI_OVERRIDE

    @Enumerated(EnumType.STRING)
    private ClaimStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private ClaimStatus newStatus;

    @Column(length = 2000)
    private String reason;

    @Column(length = 50)
    private String previousAIClassification;

    @Column(length = 50)
    private String newClassification;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
