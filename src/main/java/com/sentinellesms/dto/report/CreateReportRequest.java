package com.sentinellesms.dto.report;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateReportRequest {

    private boolean anonymous;

    @NotBlank
    private String language;

    @NotBlank
    private String category;

    @NotBlank
    private String riskLevel;

    private String reasonCodes;

    private String patternHash;

    private String modelVersion;
}
