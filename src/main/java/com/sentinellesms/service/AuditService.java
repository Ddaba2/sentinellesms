package com.sentinellesms.service;

import com.sentinellesms.dto.audit.AuditLogResponse;
import com.sentinellesms.entity.AuditLog;
import com.sentinellesms.entity.User;
import com.sentinellesms.repository.AuditLogRepository;
import com.sentinellesms.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(String action, String resourceType, String resourceId, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
        User actor = userRepository.findByUsername(username).orElse(null);

        AuditLog log = new AuditLog();
        log.setActorId(actor != null ? actor.getId() : null);
        log.setActorUsername(username);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setDetails(details);
        log.setIpAddress(resolveClientIp());
        auditLogRepository.save(log);
    }

    public Page<AuditLogResponse> list(String username, Pageable pageable) {
        Page<AuditLog> page = (username == null || username.isBlank())
                ? auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                : auditLogRepository.findByActorUsernameContainingIgnoreCaseOrderByCreatedAtDesc(username, pageable);
        return page.map(this::toResponse);
    }

    private String resolveClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .actorUsername(log.getActorUsername())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
