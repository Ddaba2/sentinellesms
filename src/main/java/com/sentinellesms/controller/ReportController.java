package com.sentinellesms.controller;

import com.sentinellesms.dto.pattern.FraudPatternResponse;
import com.sentinellesms.dto.report.CreateReportRequest;
import com.sentinellesms.dto.report.PromoteReportRequest;
import com.sentinellesms.dto.report.ReportResponse;
import com.sentinellesms.dto.report.TrendingReportResponse;
import com.sentinellesms.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public List<ReportResponse> list() {
        return reportService.listReports();
    }

    @GetMapping("/trending")
    public List<TrendingReportResponse> trending() {
        return reportService.listTrending();
    }

    @PostMapping("/promote")
    public ResponseEntity<FraudPatternResponse> promote(@Valid @RequestBody PromoteReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.promote(request));
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }
}
