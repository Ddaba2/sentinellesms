package com.sentinellesms.service;

import com.sentinellesms.dto.link.LinkCheckResponse;
import com.sentinellesms.dto.link.LinkReputationRequest;
import com.sentinellesms.dto.link.LinkReputationResponse;
import com.sentinellesms.entity.LinkReputation;
import com.sentinellesms.entity.LinkVerdict;
import com.sentinellesms.exception.BadRequestException;
import com.sentinellesms.exception.ResourceNotFoundException;
import com.sentinellesms.repository.LinkReputationRepository;
import com.sentinellesms.util.NormalizationUtils;
import com.sentinellesms.util.RiskScoreBands;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkService {

    private static final Pattern IP_HOST = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final List<String> SHORTENERS = List.of(
            "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "cutt.ly"
    );

    private final LinkReputationRepository linkReputationRepository;
    private final AuditService auditService;

    public LinkCheckResponse check(String rawUrl) {
        String normalized = NormalizationUtils.normalizeUrl(rawUrl);
        String domain = NormalizationUtils.extractDomain(rawUrl);
        if (normalized == null || domain == null) {
            throw new BadRequestException("URL invalide");
        }

        List<String> signals = new ArrayList<>();
        int score = 10;
        LinkVerdict verdict = LinkVerdict.SAFE;
        boolean known = false;
        UUID reputationId = null;
        int confidence = 0;

        Optional<LinkReputation> byUrl = linkReputationRepository.findByNormalizedUrl(normalized);
        Optional<LinkReputation> byDomain = linkReputationRepository.findByDomainAndActiveTrue(domain);

        LinkReputation knownRep = byUrl.filter(LinkReputation::isActive)
                .or(() -> byDomain)
                .orElse(null);

        if (knownRep != null) {
            known = true;
            reputationId = knownRep.getId();
            confidence = knownRep.getConfidence();
            verdict = knownRep.getVerdict();
            score = switch (verdict) {
                case SAFE -> 15;
                case SUSPECT -> Math.max(55, confidence);
                case DANGEROUS -> Math.max(85, confidence);
            };
            signals.add("Lien connu dans la base communautaire (" + verdict + ")");
            if (knownRep.getReason() != null && !knownRep.getReason().isBlank()) {
                signals.add(knownRep.getReason());
            }
        }

        if (IP_HOST.matcher(domain).matches()) {
            score = Math.max(score, 75);
            verdict = worse(verdict, LinkVerdict.DANGEROUS);
            signals.add("L'hôte est une adresse IP (souvent utilisé pour le phishing)");
        }
        if (domain.contains("-") && domain.split("\\.").length > 3) {
            score = Math.max(score, 50);
            verdict = worse(verdict, LinkVerdict.SUSPECT);
            signals.add("Sous-domaines nombreux / nom de domaine trompeur");
        }
        if (SHORTENERS.stream().anyMatch(domain::endsWith)) {
            score = Math.max(score, 60);
            verdict = worse(verdict, LinkVerdict.SUSPECT);
            signals.add("Raccourcisseur d'URL (destination masquée)");
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("login") || lower.contains("verify") || lower.contains("secure")
                || lower.contains("orange") || lower.contains("moov") || lower.contains("malitel")
                || lower.contains("wave") || lower.contains("otp")) {
            score = Math.max(score, 55);
            verdict = worse(verdict, LinkVerdict.SUSPECT);
            signals.add("Mots-clés sensibles dans l'URL (usurpation possible)");
        }
        if (normalized.contains("@")) {
            score = Math.max(score, 80);
            verdict = worse(verdict, LinkVerdict.DANGEROUS);
            signals.add("Caractère @ dans l'URL (technique de phishing classique)");
        }

        score = RiskScoreBands.clamp(score);
        String recommendation = switch (verdict) {
            case SAFE -> "Le lien semble sûr, mais restez prudent.";
            case SUSPECT -> "Lien suspect : ne cliquez pas et ne saisissez aucune information.";
            case DANGEROUS -> "Lien dangereux : ne l'ouvrez pas et signalez-le si besoin.";
        };

        return LinkCheckResponse.builder()
                .url(rawUrl)
                .normalizedUrl(normalized)
                .domain(domain)
                .verdict(verdict)
                .confidence(confidence)
                .riskScore(score)
                .riskBand(RiskScoreBands.band(score))
                .signals(signals)
                .recommendation(recommendation)
                .knownInDatabase(known)
                .reputationId(reputationId)
                .build();
    }

    public List<LinkReputationResponse> listAll() {
        return linkReputationRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public LinkReputationResponse upsert(LinkReputationRequest request) {
        String normalized = NormalizationUtils.normalizeUrl(request.getUrl());
        String domain = NormalizationUtils.extractDomain(request.getUrl());
        if (normalized == null || domain == null) {
            throw new BadRequestException("URL invalide");
        }

        LinkReputation entity = linkReputationRepository.findByNormalizedUrl(normalized)
                .orElseGet(LinkReputation::new);
        boolean created = entity.getId() == null;
        entity.setNormalizedUrl(normalized);
        entity.setDomain(domain);
        entity.setVerdict(request.getVerdict());
        entity.setConfidence(RiskScoreBands.clamp(request.getConfidence()));
        entity.setReason(request.getReason());
        entity.setActive(request.isActive());
        if (created) {
            entity.setReportCount(1);
        }
        entity.setLastReportedAt(LocalDateTime.now());
        entity = linkReputationRepository.save(entity);

        auditService.log(created ? "LINK_CREATE" : "LINK_UPDATE", "LinkReputation",
                entity.getId().toString(), "verdict=" + entity.getVerdict());
        return toResponse(entity);
    }

    @Transactional
    public void deactivate(UUID id) {
        LinkReputation entity = linkReputationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lien introuvable: " + id));
        entity.setActive(false);
        linkReputationRepository.save(entity);
        auditService.log("LINK_DEACTIVATE", "LinkReputation", id.toString(), null);
    }

    @Transactional
    public void reinforceFromValidatedReport(String rawUrl, String category, int confidence) {
        String normalized = NormalizationUtils.normalizeUrl(rawUrl);
        String domain = NormalizationUtils.extractDomain(rawUrl);
        if (normalized == null || domain == null) {
            return;
        }
        LinkReputation entity = linkReputationRepository.findByNormalizedUrl(normalized)
                .orElseGet(LinkReputation::new);
        if (entity.getId() == null) {
            entity.setNormalizedUrl(normalized);
            entity.setDomain(domain);
            entity.setReportCount(0);
            entity.setVerdict(LinkVerdict.DANGEROUS);
        }
        entity.setReportCount(entity.getReportCount() + 1);
        entity.setConfidence(RiskScoreBands.clamp(Math.max(entity.getConfidence(), confidence)));
        entity.setVerdict(entity.getConfidence() >= 70 ? LinkVerdict.DANGEROUS : LinkVerdict.SUSPECT);
        entity.setReason(category);
        entity.setActive(true);
        entity.setLastReportedAt(LocalDateTime.now());
        linkReputationRepository.save(entity);
    }

    private LinkVerdict worse(LinkVerdict current, LinkVerdict candidate) {
        if (current == LinkVerdict.DANGEROUS || candidate == LinkVerdict.DANGEROUS) {
            return LinkVerdict.DANGEROUS;
        }
        if (current == LinkVerdict.SUSPECT || candidate == LinkVerdict.SUSPECT) {
            return LinkVerdict.SUSPECT;
        }
        return LinkVerdict.SAFE;
    }

    private LinkReputationResponse toResponse(LinkReputation entity) {
        return LinkReputationResponse.builder()
                .id(entity.getId())
                .normalizedUrl(entity.getNormalizedUrl())
                .domain(entity.getDomain())
                .verdict(entity.getVerdict())
                .confidence(entity.getConfidence())
                .reportCount(entity.getReportCount())
                .reason(entity.getReason())
                .active(entity.isActive())
                .firstReportedAt(entity.getFirstReportedAt())
                .lastReportedAt(entity.getLastReportedAt())
                .build();
    }
}
