package com.claimwise.repository;

import com.claimwise.entity.ReviewDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewDecisionRepository extends JpaRepository<ReviewDecision, Long> {
    List<ReviewDecision> findByClaimIdOrderByCreatedAtDesc(Long claimId);
}
