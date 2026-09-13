package com.sentinellesms.service;

import com.sentinellesms.dto.pattern.FraudPatternRequest;
import com.sentinellesms.dto.pattern.FraudPatternResponse;
import com.sentinellesms.entity.FraudPattern;
import com.sentinellesms.exception.ResourceNotFoundException;
import com.sentinellesms.repository.FraudPatternRepository;
import com.sentinellesms.util.RiskScoreBands;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatternService {

    private final FraudPatternRepository fraudPatternRepository;
    private final AuditService auditService;

    public List<FraudPatternResponse> listActive(String language) {
        List<FraudPattern> patterns = (language == null || language.isBlank())
                ? fraudPatternRepository.findByActiveTrue()
                : fraudPatternRepository.findByActiveTrueAndLanguage(language);
        return patterns.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<FraudPatternResponse> listAll() {
        return fraudPatternRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public FraudPatternResponse create(FraudPatternRequest request) {
        FraudPattern pattern = new FraudPattern();
        applyRequest(pattern, request);
        pattern = fraudPatternRepository.save(pattern);
        auditService.log("PATTERN_CREATE", "FraudPattern", pattern.getId().toString(), pattern.getLabel());
        return toResponse(pattern);
    }

    public FraudPatternResponse update(UUID id, FraudPatternRequest request) {
        FraudPattern pattern = fraudPatternRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motif de fraude introuvable: " + id));
        applyRequest(pattern, request);
        pattern = fraudPatternRepository.save(pattern);
        auditService.log("PATTERN_UPDATE", "FraudPattern", id.toString(), pattern.getLabel());
        return toResponse(pattern);
    }

    public void delete(UUID id) {
        if (!fraudPatternRepository.existsById(id)) {
            throw new ResourceNotFoundException("Motif de fraude introuvable: " + id);
        }
        fraudPatternRepository.deleteById(id);
        auditService.log("PATTERN_DELETE", "FraudPattern", id.toString(), null);
    }

    private void applyRequest(FraudPattern pattern, FraudPatternRequest request) {
        pattern.setLabel(request.getLabel());
        pattern.setKeywords(request.getKeywords());
        pattern.setLanguage(request.getLanguage());
        pattern.setCategory(request.getCategory());
        pattern.setRiskLevel(request.getRiskLevel());
        pattern.setRiskScore(request.getRiskScore() != null
                ? RiskScoreBands.clamp(request.getRiskScore())
                : RiskScoreBands.fromRiskLevel(request.getRiskLevel()));
        pattern.setDescription(request.getDescription());
        pattern.setSignalCodes(request.getSignalCodes());
        pattern.setActive(request.isActive());
    }

    private FraudPatternResponse toResponse(FraudPattern pattern) {
        return FraudPatternResponse.builder()
                .id(pattern.getId())
                .label(pattern.getLabel())
                .keywords(pattern.getKeywords())
                .language(pattern.getLanguage())
                .category(pattern.getCategory())
                .riskLevel(pattern.getRiskLevel())
                .riskScore(pattern.getRiskScore())
                .description(pattern.getDescription())
                .signalCodes(pattern.getSignalCodes())
                .active(pattern.isActive())
                .createdAt(pattern.getCreatedAt())
                .updatedAt(pattern.getUpdatedAt())
                .build();
    }
}
