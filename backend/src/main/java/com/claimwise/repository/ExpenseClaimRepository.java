package com.claimwise.repository;

import com.claimwise.entity.ClaimStatus;
import com.claimwise.entity.ExpenseCategory;
import com.claimwise.entity.ExpenseClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, Long>, JpaSpecificationExecutor<ExpenseClaim> {

    @Query("SELECT c FROM ExpenseClaim c JOIN FETCH c.claimant WHERE c.id = :id")
    Optional<ExpenseClaim> findByIdWithClaimant(@Param("id") Long id);

    @Query("SELECT c FROM ExpenseClaim c JOIN FETCH c.claimant WHERE c.claimant.id = :claimantId ORDER BY c.createdAt DESC")
    List<ExpenseClaim> findByClaimantIdOrderByCreatedAtDesc(@Param("claimantId") Long claimantId);

    @Query("SELECT c FROM ExpenseClaim c JOIN FETCH c.claimant ORDER BY c.createdAt DESC")
    List<ExpenseClaim> findAllByOrderByCreatedAtDesc();

    @Query("SELECT c FROM ExpenseClaim c JOIN FETCH c.claimant WHERE c.status = :status ORDER BY c.createdAt DESC")
    List<ExpenseClaim> findByStatusOrderByCreatedAtDesc(@Param("status") ClaimStatus status);

    @Query("SELECT c FROM ExpenseClaim c JOIN FETCH c.claimant WHERE c.category = :category ORDER BY c.createdAt DESC")
    List<ExpenseClaim> findByCategoryOrderByCreatedAtDesc(@Param("category") ExpenseCategory category);
}
