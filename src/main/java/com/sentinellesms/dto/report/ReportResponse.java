package com.sentinellesms.dto.report;

import com.sentinellesms.entity.ModerationStatus;
import com.sentinellesms.entity.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private UUID id;
    private boolean anonymous;
    private String language;
    private String category;
    private String riskLevel;
    private Integer riskScore;
    private String reasonCodes;
    private String patternHash;
    private String modelVersion;
    private ReportType reportType;
    private String targetValue;
    private ModerationStatus status;
    private Integer confidence;
    private boolean reviewed;
    private String reviewNotes;
    private UUID mergedIntoId;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
