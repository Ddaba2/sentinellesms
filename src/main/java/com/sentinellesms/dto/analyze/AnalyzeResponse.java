package com.sentinellesms.dto.analyze;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeResponse {

    private int riskScore;
    private String riskBand;
    private String riskLevel;
    private boolean highRiskAlert;
    private String recommendation;
    private List<DetectedSignal> signals;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectedSignal {
        private String code;
        private String label;
        private String explanation;
        private int contribution;
    }
}
