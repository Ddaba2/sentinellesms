package com.sentinellesms.dto.pattern;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Min(0)
    @Max(100)
    private Integer riskScore;

    private String description;

    /** Codes de signaux (urgence,pin,otp,lien,gain,...). */
    private String signalCodes;

    private boolean active = true;
}
