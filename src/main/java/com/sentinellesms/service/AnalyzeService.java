package com.sentinellesms.service;

import com.sentinellesms.dto.analyze.AnalyzeRequest;
import com.sentinellesms.dto.analyze.AnalyzeResponse;
import com.sentinellesms.dto.link.LinkCheckResponse;
import com.sentinellesms.entity.FraudPattern;
import com.sentinellesms.entity.LinkVerdict;
import com.sentinellesms.entity.PhoneReputation;
import com.sentinellesms.repository.FraudPatternRepository;
import com.sentinellesms.repository.PhoneReputationRepository;
import com.sentinellesms.util.NormalizationUtils;
import com.sentinellesms.util.RiskScoreBands;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyzeService {

    private static final Map<String, String> SIGNAL_LABELS = Map.ofEntries(
            Map.entry("urgence", "Urgence artificielle"),
            Map.entry("argent", "Demande d'argent"),
            Map.entry("pin", "Demande de PIN"),
            Map.entry("otp", "Demande de code OTP"),
            Map.entry("lien", "Lien suspect"),
            Map.entry("gain", "Faux gain / concours"),
            Map.entry("remboursement", "Faux remboursement"),
            Map.entry("blocage", "Faux blocage de compte"),
            Map.entry("agent", "Faux agent Mobile Money"),
            Map.entry("transfert", "Faux transfert"),
            Map.entry("expéditeur", "Expéditeur inhabituel"),
            Map.entry("numero", "Numéro signalé"),
            Map.entry("cni", "Demande de pièce d'identité")
    );

    private final FraudPatternRepository fraudPatternRepository;
    private final PhoneReputationRepository phoneReputationRepository;
    private final LinkService linkService;

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        Map<String, AnalyzeResponse.DetectedSignal> signals = new LinkedHashMap<>();
        int score = 0;

        if (request.getText() != null && !request.getText().isBlank()) {
            score = Math.max(score, analyzeText(request.getText(), request.getLanguage(), signals));
        }
        if (request.getUrl() != null && !request.getUrl().isBlank()) {
            score = Math.max(score, analyzeUrl(request.getUrl(), signals));
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            score = Math.max(score, analyzePhone(request.getPhone(), signals));
        }

        score = RiskScoreBands.clamp(score);
        boolean highRisk = score >= 61;
        String recommendation = highRisk
                ? "Ne communiquez jamais votre PIN, OTP, mot de passe ou données sensibles. Ne cliquez pas et ne payez pas."
                : score >= 31
                ? "Restez vigilant : vérifiez l'expéditeur et ne partagez aucune information personnelle."
                : "Risque faible détecté. Restez prudent face aux demandes inhabituelles.";

        return AnalyzeResponse.builder()
                .riskScore(score)
                .riskBand(RiskScoreBands.band(score))
                .riskLevel(RiskScoreBands.riskLevelFromScore(score))
                .highRiskAlert(highRisk)
                .recommendation(recommendation)
                .signals(new ArrayList<>(signals.values()))
                .build();
    }

    private int analyzeText(String text, String language, Map<String, AnalyzeResponse.DetectedSignal> signals) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<FraudPattern> patterns = (language == null || language.isBlank())
                ? fraudPatternRepository.findByActiveTrue()
                : fraudPatternRepository.findByActiveTrueAndLanguage(language);

        int max = 0;
        for (FraudPattern pattern : patterns) {
            String[] keywords = pattern.getKeywords().split("[,;|]");
            boolean matched = false;
            for (String keyword : keywords) {
                String k = keyword.trim().toLowerCase(Locale.ROOT);
                if (!k.isEmpty() && lower.contains(k)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                continue;
            }
            int contribution = pattern.getRiskScore() > 0
                    ? pattern.getRiskScore()
                    : RiskScoreBands.fromRiskLevel(pattern.getRiskLevel());
            max = Math.max(max, contribution);
            addSignalsFromPattern(pattern, contribution, signals);
        }

        // Heuristiques Mobile Money / fraude courante
        max = Math.max(max, applyHeuristic(lower, "pin|code secret|code orange|code moov|code malitel", "pin", 75, signals));
        max = Math.max(max, applyHeuristic(lower, "otp|code de confirmation|code reçu", "otp", 80, signals));
        max = Math.max(max, applyHeuristic(lower, "urgent|immédiatement|dans 5 minutes|compte bloqué", "urgence", 55, signals));
        max = Math.max(max, applyHeuristic(lower, "gagnez|félicitations|vous avez gagné|concours", "gain", 65, signals));
        max = Math.max(max, applyHeuristic(lower, "remboursement|trop perçu|erreur de transfert", "remboursement", 70, signals));
        max = Math.max(max, applyHeuristic(lower, "agent orange|agent moov|agent malitel|agent wave", "agent", 70, signals));
        max = Math.max(max, applyHeuristic(lower, "cni|carte d'identité|passeport|photo de votre", "cni", 60, signals));

        return max;
    }

    private int analyzeUrl(String url, Map<String, AnalyzeResponse.DetectedSignal> signals) {
        LinkCheckResponse check = linkService.check(url);
        String code = "lien";
        String explanation = check.getSignals() == null || check.getSignals().isEmpty()
                ? "Analyse du lien: " + check.getVerdict()
                : String.join("; ", check.getSignals());
        signals.put(code, AnalyzeResponse.DetectedSignal.builder()
                .code(code)
                .label(SIGNAL_LABELS.get(code))
                .explanation(explanation)
                .contribution(check.getRiskScore())
                .build());
        return check.getRiskScore();
    }

    private int analyzePhone(String phone, Map<String, AnalyzeResponse.DetectedSignal> signals) {
        String normalized = NormalizationUtils.normalizePhone(phone);
        if (normalized == null) {
            return 0;
        }
        return phoneReputationRepository.findByNormalizedPhone(normalized)
                .filter(PhoneReputation::isActive)
                .map(rep -> {
                    int score = RiskScoreBands.clamp(Math.max(50, rep.getConfidence()));
                    signals.put("numero", AnalyzeResponse.DetectedSignal.builder()
                            .code("numero")
                            .label(SIGNAL_LABELS.get("numero"))
                            .explanation("Numéro signalé pour: " + rep.getCategory()
                                    + " (confiance " + rep.getConfidence() + "%, "
                                    + rep.getReportCount() + " signalement(s))")
                            .contribution(score)
                            .build());
                    return score;
                })
                .orElse(0);
    }

    private void addSignalsFromPattern(FraudPattern pattern, int contribution,
                                       Map<String, AnalyzeResponse.DetectedSignal> signals) {
        String codes = pattern.getSignalCodes();
        if (codes == null || codes.isBlank()) {
            String fallback = pattern.getCategory() == null ? "fraude" : pattern.getCategory().toLowerCase(Locale.ROOT);
            signals.putIfAbsent(fallback, AnalyzeResponse.DetectedSignal.builder()
                    .code(fallback)
                    .label(pattern.getLabel())
                    .explanation(pattern.getDescription() != null ? pattern.getDescription() : pattern.getLabel())
                    .contribution(contribution)
                    .build());
            return;
        }
        for (String raw : codes.split("[,;|]")) {
            String code = raw.trim().toLowerCase(Locale.ROOT);
            if (code.isEmpty()) {
                continue;
            }
            signals.putIfAbsent(code, AnalyzeResponse.DetectedSignal.builder()
                    .code(code)
                    .label(SIGNAL_LABELS.getOrDefault(code, pattern.getLabel()))
                    .explanation(pattern.getDescription() != null ? pattern.getDescription() : pattern.getLabel())
                    .contribution(contribution)
                    .build());
        }
    }

    private int applyHeuristic(String text, String regex, String code, int contribution,
                               Map<String, AnalyzeResponse.DetectedSignal> signals) {
        if (!text.matches("(?s).*(?i)(" + regex + ").*")) {
            return 0;
        }
        signals.putIfAbsent(code, AnalyzeResponse.DetectedSignal.builder()
                .code(code)
                .label(SIGNAL_LABELS.getOrDefault(code, code))
                .explanation("Signal détecté dans le message: " + SIGNAL_LABELS.getOrDefault(code, code))
                .contribution(contribution)
                .build());
        return contribution;
    }
}
