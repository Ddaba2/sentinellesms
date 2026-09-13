package com.sentinellesms.util;

public final class RiskScoreBands {

    private RiskScoreBands() {
    }

    public static String band(int score) {
        int clamped = clamp(score);
        if (clamped <= 30) {
            return "FAIBLE";
        }
        if (clamped <= 60) {
            return "MODERE";
        }
        if (clamped <= 80) {
            return "ELEVE";
        }
        return "CRITIQUE";
    }

    public static String riskLevelFromScore(int score) {
        return switch (band(score)) {
            case "FAIBLE" -> "LOW";
            case "MODERE" -> "MEDIUM";
            case "ELEVE" -> "HIGH";
            default -> "CRITICAL";
        };
    }

    public static int fromRiskLevel(String riskLevel) {
        if (riskLevel == null) {
            return 50;
        }
        return switch (riskLevel.trim().toUpperCase()) {
            case "LOW", "FAIBLE" -> 20;
            case "MEDIUM", "MODERE", "MODÉRÉ" -> 45;
            case "HIGH", "ELEVE", "ÉLEVÉ" -> 70;
            case "CRITICAL", "CRITIQUE" -> 90;
            default -> 50;
        };
    }

    public static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
