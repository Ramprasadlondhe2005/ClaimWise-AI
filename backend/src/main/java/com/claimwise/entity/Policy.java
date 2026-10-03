package com.claimwise.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String policyCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 2000)
    private String ruleText;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal maximumAmount;

    @Column(nullable = false)
    private boolean receiptRequired;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
