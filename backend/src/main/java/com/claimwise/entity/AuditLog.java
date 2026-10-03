package com.claimwise.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long claimId;

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false, length = 100)
    private String actorName;

    @Column(nullable = false, length = 50)
    private String actorRole;

    @Column(nullable = false, length = 100)
    private String action; // CLAIM_CREATED, VALIDATION_COMPLETED, AI_REVIEW_STARTED, AI_REVIEW_COMPLETED, AI_REVIEW_FAILED, etc.

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 2000)
    private String metadata;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}
