package com.sentinellesms.controller;

import com.sentinellesms.dto.audit.AuditLogResponse;
import com.sentinellesms.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public Page<AuditLogResponse> list(@RequestParam(required = false) String username,
                                       @PageableDefault(size = 50) Pageable pageable) {
        return auditService.list(username, pageable);
    }
}
