package com.claimwise.repository;

import com.claimwise.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByClaimIdOrderByTimestampDesc(Long claimId);
}
