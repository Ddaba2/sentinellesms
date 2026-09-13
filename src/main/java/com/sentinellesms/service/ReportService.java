package com.sentinellesms.service;

import com.sentinellesms.dto.pattern.FraudPatternRequest;
import com.sentinellesms.dto.pattern.FraudPatternResponse;
import com.sentinellesms.dto.report.CreateReportRequest;
import com.sentinellesms.dto.report.ModerateReportRequest;
import com.sentinellesms.dto.report.PromoteReportRequest;
import com.sentinellesms.dto.report.ReportResponse;
import com.sentinellesms.dto.report.TrendingReportResponse;
import com.sentinellesms.entity.ModerationStatus;
import com.sentinellesms.entity.Report;
import com.sentinellesms.entity.ReportType;
import com.sentinellesms.entity.User;
import com.sentinellesms.exception.BadRequestException;
import com.sentinellesms.exception.ResourceNotFoundException;
import com.sentinellesms.repository.ReportRepository;
import com.sentinellesms.repository.UserRepository;
import com.sentinellesms.util.NormalizationUtils;
import com.sentinellesms.util.RiskScoreBands;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PatternService patternService;
    private final PhoneService phoneService;
    private final LinkService linkService;
    private final AuditService auditService;

    public ReportResponse createReport(CreateReportRequest request, String username) {
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;

        ReportType type = request.getReportType() != null ? request.getReportType() : ReportType.MESSAGE;
        String target = normalizeTarget(type, request.getTargetValue());

        Report report = new Report();
        report.setUser(request.isAnonymous() ? null : user);
        report.setAnonymous(request.isAnonymous() || user == null);
        report.setLanguage(request.getLanguage());
        report.setCategory(request.getCategory());
        report.setRiskLevel(request.getRiskLevel());
        report.setRiskScore(request.getRiskScore() != null
                ? RiskScoreBands.clamp(request.getRiskScore())
                : RiskScoreBands.fromRiskLevel(request.getRiskLevel()));
        report.setReasonCodes(request.getReasonCodes());
        report.setPatternHash(request.getPatternHash());
        report.setModelVersion(request.getModelVersion());
        report.setReportType(type);
        report.setTargetValue(target);
        report.setStatus(ModerationStatus.PENDING);
        report.setReviewed(false);

        report = reportRepository.save(report);
        return toResponse(report);
    }

    public List<ReportResponse> listReports(ModerationStatus status, ReportType type) {
        List<Report> reports;
        if (status != null && type != null) {
            reports = reportRepository.findByStatusAndReportTypeOrderByCreatedAtAsc(status, type);
        } else if (status != null) {
            reports = reportRepository.findByStatusOrderByCreatedAtAsc(status);
        } else {
            reports = reportRepository.findAll();
        }
        return reports.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ReportResponse> pendingQueue(ReportType type) {
        return listReports(ModerationStatus.PENDING, type);
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
        patternRequest.setRiskScore(reference.getRiskScore() != null
                ? reference.getRiskScore()
                : RiskScoreBands.fromRiskLevel(reference.getRiskLevel()));
        patternRequest.setActive(true);

        FraudPatternResponse created = patternService.create(patternRequest);

        UUID actorId = currentUserId();
        matches.forEach(r -> {
            r.setReviewed(true);
            r.setStatus(ModerationStatus.VALIDATED);
            r.setReviewedBy(actorId);
            r.setReviewedAt(LocalDateTime.now());
            r.setConfidence(80);
        });
        reportRepository.saveAll(matches);

        auditService.log("REPORT_PROMOTE", "Report", request.getPatternHash(),
                "patternId=" + created.getId());
        return created;
    }

    @Transactional
    public ReportResponse validate(UUID id, ModerateReportRequest request) {
        Report report = getReport(id);
        ensurePending(report);

        int confidence = request.getConfidence() != null ? RiskScoreBands.clamp(request.getConfidence()) : 70;
        report.setStatus(ModerationStatus.VALIDATED);
        report.setReviewed(true);
        report.setConfidence(confidence);
        report.setReviewNotes(request.getReviewNotes());
        report.setReviewedBy(currentUserId());
        report.setReviewedAt(LocalDateTime.now());
        reportRepository.save(report);

        if (report.getReportType() == ReportType.PHONE && report.getTargetValue() != null) {
            phoneService.reinforceFromValidatedReport(report.getTargetValue(), report.getCategory(), confidence);
        }
        if (report.getReportType() == ReportType.LINK && report.getTargetValue() != null) {
            linkService.reinforceFromValidatedReport(report.getTargetValue(), report.getCategory(), confidence);
        }

        auditService.log("REPORT_VALIDATE", "Report", id.toString(), "confidence=" + confidence);
        return toResponse(report);
    }

    @Transactional
    public ReportResponse reject(UUID id, ModerateReportRequest request) {
        Report report = getReport(id);
        ensurePending(report);

        report.setStatus(ModerationStatus.REJECTED);
        report.setReviewed(true);
        report.setConfidence(request.getConfidence());
        report.setReviewNotes(request.getReviewNotes());
        report.setReviewedBy(currentUserId());
        report.setReviewedAt(LocalDateTime.now());
        reportRepository.save(report);

        auditService.log("REPORT_REJECT", "Report", id.toString(), request.getReviewNotes());
        return toResponse(report);
    }

    @Transactional
    public ReportResponse merge(UUID id, ModerateReportRequest request) {
        Report report = getReport(id);
        ensurePending(report);
        if (request.getMergeIntoId() == null) {
            throw new BadRequestException("mergeIntoId est requis pour une fusion");
        }
        Report target = getReport(request.getMergeIntoId());

        report.setStatus(ModerationStatus.MERGED);
        report.setReviewed(true);
        report.setMergedIntoId(target.getId());
        report.setReviewNotes(request.getReviewNotes());
        report.setReviewedBy(currentUserId());
        report.setReviewedAt(LocalDateTime.now());
        report.setConfidence(request.getConfidence() != null ? request.getConfidence() : target.getConfidence());
        reportRepository.save(report);

        auditService.log("REPORT_MERGE", "Report", id.toString(), "mergedInto=" + target.getId());
        return toResponse(report);
    }

    private void ensurePending(Report report) {
        if (report.getStatus() != ModerationStatus.PENDING) {
            throw new BadRequestException("Ce signalement a déjà été traité (" + report.getStatus() + ")");
        }
    }

    private Report getReport(UUID id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable: " + id));
    }

    private String normalizeTarget(ReportType type, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (type) {
            case PHONE -> {
                String phone = NormalizationUtils.normalizePhone(raw);
                if (phone == null) {
                    throw new BadRequestException("Numéro invalide pour le signalement");
                }
                yield phone;
            }
            case LINK -> {
                String url = NormalizationUtils.normalizeUrl(raw);
                if (url == null) {
                    throw new BadRequestException("URL invalide pour le signalement");
                }
                yield url;
            }
            case MESSAGE -> raw.trim().length() > 500 ? raw.trim().substring(0, 500) : raw.trim();
        };
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).map(User::getId).orElse(null);
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
                .riskScore(report.getRiskScore())
                .reasonCodes(report.getReasonCodes())
                .patternHash(report.getPatternHash())
                .modelVersion(report.getModelVersion())
                .reportType(report.getReportType())
                .targetValue(report.getTargetValue())
                .status(report.getStatus())
                .confidence(report.getConfidence())
                .reviewed(report.isReviewed())
                .reviewNotes(report.getReviewNotes())
                .mergedIntoId(report.getMergedIntoId())
                .createdAt(report.getCreatedAt())
                .reviewedAt(report.getReviewedAt())
                .build();
    }
}
