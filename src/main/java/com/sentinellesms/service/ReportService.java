package com.sentinellesms.service;

import com.sentinellesms.dto.pattern.FraudPatternRequest;
import com.sentinellesms.dto.pattern.FraudPatternResponse;
import com.sentinellesms.dto.report.CreateReportRequest;
import com.sentinellesms.dto.report.PromoteReportRequest;
import com.sentinellesms.dto.report.ReportResponse;
import com.sentinellesms.dto.report.TrendingReportResponse;
import com.sentinellesms.entity.Report;
import com.sentinellesms.entity.User;
import com.sentinellesms.exception.ResourceNotFoundException;
import com.sentinellesms.repository.ReportRepository;
import com.sentinellesms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PatternService patternService;

    public ReportResponse createReport(CreateReportRequest request, String username) {
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;

        Report report = new Report();
        report.setUser(request.isAnonymous() ? null : user);
        report.setAnonymous(request.isAnonymous() || user == null);
        report.setLanguage(request.getLanguage());
        report.setCategory(request.getCategory());
        report.setRiskLevel(request.getRiskLevel());
        report.setReasonCodes(request.getReasonCodes());
        report.setPatternHash(request.getPatternHash());
        report.setModelVersion(request.getModelVersion());

        report = reportRepository.save(report);
        return toResponse(report);
    }

    public List<ReportResponse> listReports() {
        return reportRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TrendingReportResponse> listTrending() {
        return reportRepository.findTrendingClusters().stream()
                .map(this::toTrendingResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FraudPatternResponse promote(PromoteReportRequest request) {
        List<Report> matches = reportRepository.findByPatternHashAndReviewedFalse(request.getPatternHash());
        if (matches.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Aucun signalement non traité pour ce hash: " + request.getPatternHash());
        }

        Report reference = matches.get(0);

        FraudPatternRequest patternRequest = new FraudPatternRequest();
        patternRequest.setLabel(request.getLabel());
        patternRequest.setKeywords(request.getKeywords());
        patternRequest.setDescription(request.getDescription());
        patternRequest.setLanguage(reference.getLanguage());
        patternRequest.setCategory(reference.getCategory());
        patternRequest.setRiskLevel(reference.getRiskLevel());
        patternRequest.setActive(true);

        FraudPatternResponse created = patternService.create(patternRequest);

        matches.forEach(r -> r.setReviewed(true));
        reportRepository.saveAll(matches);

        return created;
    }

    private TrendingReportResponse toTrendingResponse(Object[] row) {
        return TrendingReportResponse.builder()
                .patternHash((String) row[0])
                .category((String) row[1])
                .riskLevel((String) row[2])
                .language((String) row[3])
                .occurrences((Long) row[4])
                .firstReportedAt((LocalDateTime) row[5])
                .lastReportedAt((LocalDateTime) row[6])
                .build();
    }

    private ReportResponse toResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .anonymous(report.isAnonymous())
                .language(report.getLanguage())
                .category(report.getCategory())
                .riskLevel(report.getRiskLevel())
                .reasonCodes(report.getReasonCodes())
                .patternHash(report.getPatternHash())
                .modelVersion(report.getModelVersion())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
