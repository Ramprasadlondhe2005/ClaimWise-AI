package com.claimwise.repository;

import com.claimwise.entity.ExpenseCategory;
import com.claimwise.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByCategoryAndActiveTrue(ExpenseCategory category);
    Optional<Policy> findByPolicyCode(String policyCode);
    List<Policy> findByActiveTrue();
}
