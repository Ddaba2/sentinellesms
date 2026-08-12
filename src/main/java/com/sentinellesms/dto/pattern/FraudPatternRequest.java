package com.sentinellesms.dto.pattern;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FraudPatternRequest {

    @NotBlank
    private String label;

    @NotBlank
    private String keywords;

    @NotBlank
    private String language;

    @NotBlank
    private String category;

    @NotBlank
    private String riskLevel;

    private String description;

    private boolean active = true;
}
