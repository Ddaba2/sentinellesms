package com.sentinellesms.service;

import com.sentinellesms.dto.phone.PhoneLookupResponse;
import com.sentinellesms.dto.phone.PhoneReputationResponse;
import com.sentinellesms.entity.ModerationStatus;
import com.sentinellesms.entity.PhoneReputation;
import com.sentinellesms.entity.Report;
import com.sentinellesms.entity.ReportType;
import com.sentinellesms.exception.BadRequestException;
import com.sentinellesms.exception.ResourceNotFoundException;
import com.sentinellesms.repository.PhoneReputationRepository;
import com.sentinellesms.repository.ReportRepository;
import com.sentinellesms.util.NormalizationUtils;
import com.sentinellesms.util.RiskScoreBands;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhoneService {

    private final PhoneReputationRepository phoneReputationRepository;
    private final ReportRepository reportRepository;
    private final AuditService auditService;

    public PhoneLookupResponse lookup(String rawPhone) {
        String normalized = NormalizationUtils.normalizePhone(rawPhone);
        if (normalized == null) {
            throw new BadRequestException("Numéro invalide");
        }

        List<Report> history = reportRepository
                .findByTargetValueAndReportTypeOrderByCreatedAtDesc(normalized, ReportType.PHONE)
                .stream()
                .filter(r -> r.getStatus() == ModerationStatus.VALIDATED || r.getStatus() == ModerationStatus.PENDING)
                .collect(Collectors.toList());

        return phoneReputationRepository.findByNormalizedPhone(normalized)
                .filter(PhoneReputation::isActive)
                .map(rep -> PhoneLookupResponse.builder()
                        .normalizedPhone(normalized)
                        .reported(true)
                        .category(rep.getCategory())
                        .fraudTypes(rep.getFraudTypes())
                        .reportCount(rep.getReportCount())
                        .confidence(rep.getConfidence())
                        .firstReportedAt(rep.getFirstReportedAt())
                        .lastReportedAt(rep.getLastReportedAt())
                        .history(toHistory(history))
                        .build())
                .orElseGet(() -> PhoneLookupResponse.builder()
                        .normalizedPhone(normalized)
                        .reported(false)
                        .reportCount(0)
                        .confidence(0)
                        .history(toHistory(history))
                        .build());
    }

    public List<PhoneReputationResponse> listAll() {
        return phoneReputationRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void reinforceFromValidatedReport(String rawPhone, String category, int confidence) {
        String normalized = NormalizationUtils.normalizePhone(rawPhone);
        if (normalized == null) {
            return;
        }
        PhoneReputation entity = phoneReputationRepository.findByNormalizedPhone(normalized)
                .orElseGet(PhoneReputation::new);
        if (entity.getId() == null) {
            entity.setNormalizedPhone(normalized);
            entity.setReportCount(0);
            entity.setFraudTypes(category);
        } else if (entity.getFraudTypes() == null || entity.getFraudTypes().isBlank()) {
            entity.setFraudTypes(category);
        } else if (!entity.getFraudTypes().contains(category)) {
            entity.setFraudTypes(entity.getFraudTypes() + "," + category);
        }
        entity.setCategory(category);
        entity.setReportCount(entity.getReportCount() + 1);
        entity.setConfidence(RiskScoreBands.clamp(Math.max(entity.getConfidence(), confidence)));
        entity.setActive(true);
        entity.setLastReportedAt(LocalDateTime.now());
        phoneReputationRepository.save(entity);
    }

    @Transactional
    public PhoneReputationResponse deactivate(UUID id) {
        PhoneReputation entity = phoneReputationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Numéro introuvable: " + id));
        entity.setActive(false);
        phoneReputationRepository.save(entity);
        auditService.log("PHONE_DEACTIVATE", "PhoneReputation", id.toString(), null);
        return toResponse(entity);
    }

    private List<PhoneLookupResponse.PhoneReportHistoryItem> toHistory(List<Report> reports) {
        return reports.stream()
                .map(r -> PhoneLookupResponse.PhoneReportHistoryItem.builder()
                        .reportId(r.getId())
                        .category(r.getCategory())
                        .riskLevel(r.getRiskLevel())
                        .riskScore(r.getRiskScore())
                        .createdAt(r.getCreatedAt())
                        .status(r.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    private PhoneReputationResponse toResponse(PhoneReputation entity) {
        return PhoneReputationResponse.builder()
                .id(entity.getId())
                .normalizedPhone(entity.getNormalizedPhone())
                .category(entity.getCategory())
                .fraudTypes(entity.getFraudTypes())
                .reportCount(entity.getReportCount())
                .confidence(entity.getConfidence())
                .active(entity.isActive())
                .firstReportedAt(entity.getFirstReportedAt())
                .lastReportedAt(entity.getLastReportedAt())
                .build();
    }
}
