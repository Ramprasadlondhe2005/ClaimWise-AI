package com.claimwise.dto;

import com.claimwise.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private Long claimId;
    private Long actorId;
    private String actorName;
    private String actorRole;
    private String action;
    private String description;
    private String metadata;
    private LocalDateTime timestamp;

    public static AuditLogResponse fromEntity(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .claimId(log.getClaimId())
                .actorId(log.getActorId())
                .actorName(log.getActorName())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .description(log.getDescription())
                .metadata(log.getMetadata())
                .timestamp(log.getTimestamp())
                .build();
    }
}
