package com.sentinellesms.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private UUID id;
    private UUID actorId;
    private String actorUsername;
    private String action;
    private String resourceType;
    private String resourceId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
