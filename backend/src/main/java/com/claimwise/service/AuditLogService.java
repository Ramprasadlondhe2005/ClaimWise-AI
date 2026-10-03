package com.claimwise.service;

import com.claimwise.dto.AuditLogResponse;
import com.claimwise.entity.AuditLog;
import com.claimwise.entity.User;
import com.claimwise.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void recordEvent(Long claimId, User actor, String action, String description, String metadata) {
        AuditLog log = AuditLog.builder()
                .claimId(claimId)
                .actorId(actor != null ? actor.getId() : 0L)
                .actorName(actor != null ? actor.getFullName() : "SYSTEM_ENGINE")
                .actorRole(actor != null ? actor.getRole().name() : "SYSTEM")
                .action(action)
                .description(description)
                .metadata(metadata)
                .build();

        auditLogRepository.save(log);
    }

    @Transactional
    public void recordSystemEvent(Long claimId, String action, String description, String metadata) {
        AuditLog log = AuditLog.builder()
                .claimId(claimId)
                .actorId(0L)
                .actorName("ClaimWise System Engine")
                .actorRole("SYSTEM")
                .action(action)
                .description(description)
                .metadata(metadata)
                .build();

        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> getClaimAuditTrail(Long claimId) {
        return auditLogRepository.findByClaimIdOrderByTimestampDesc(claimId)
                .stream()
                .map(AuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
