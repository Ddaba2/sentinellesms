package com.sentinellesms.dto.report;

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

    private String reasonCodes;

    private String patternHash;

    private String modelVersion;

    private LocalDateTime createdAt;
}
