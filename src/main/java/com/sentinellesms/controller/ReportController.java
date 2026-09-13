package com.sentinellesms.controller;

import com.sentinellesms.dto.pattern.FraudPatternResponse;
import com.sentinellesms.dto.report.CreateReportRequest;
import com.sentinellesms.dto.report.ModerateReportRequest;
import com.sentinellesms.dto.report.PromoteReportRequest;
import com.sentinellesms.dto.report.ReportResponse;
import com.sentinellesms.dto.report.TrendingReportResponse;
import com.sentinellesms.entity.ModerationStatus;
import com.sentinellesms.entity.ReportType;
import com.sentinellesms.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponse> create(@Valid @RequestBody CreateReportRequest request,
                                                  Authentication authentication) {
        String username = resolveUsername(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createReport(request, username));
    }

    @GetMapping
    public List<ReportResponse> list(@RequestParam(required = false) ModerationStatus status,
                                     @RequestParam(required = false) ReportType type) {
        return reportService.listReports(status, type);
    }

    @GetMapping("/pending")
    public List<ReportResponse> pending(@RequestParam(required = false) ReportType type) {
        return reportService.pendingQueue(type);
    }

    @GetMapping("/trending")
    public List<TrendingReportResponse> trending() {
        return reportService.listTrending();
    }

    @PostMapping("/promote")
    public ResponseEntity<FraudPatternResponse> promote(@Valid @RequestBody PromoteReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.promote(request));
    }

    @PostMapping("/{id}/validate")
    public ReportResponse validate(@PathVariable UUID id, @RequestBody(required = false) ModerateReportRequest request) {
        return reportService.validate(id, request != null ? request : new ModerateReportRequest());
    }

    @PostMapping("/{id}/reject")
    public ReportResponse reject(@PathVariable UUID id, @RequestBody(required = false) ModerateReportRequest request) {
        return reportService.reject(id, request != null ? request : new ModerateReportRequest());
    }

    @PostMapping("/{id}/merge")
    public ReportResponse merge(@PathVariable UUID id, @Valid @RequestBody ModerateReportRequest request) {
        return reportService.merge(id, request);
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }
}
