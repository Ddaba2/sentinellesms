package com.sentinellesms.dto.report;

import com.sentinellesms.entity.ModerationStatus;
import com.sentinellesms.entity.ReportType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Min(0)
    @Max(100)
    private Integer riskScore;

    private String reasonCodes;

    private String patternHash;

    private String modelVersion;

    /** MESSAGE (défaut), LINK ou PHONE. */
    private ReportType reportType = ReportType.MESSAGE;

    /** Numéro ou URL associé au signalement (minimisé côté serveur). */
    private String targetValue;
}
