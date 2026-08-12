package com.sentinellesms.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingReportResponse {

    private String patternHash;

    private String category;

    private String riskLevel;

    private String language;

    private long occurrences;

    private LocalDateTime firstReportedAt;

    private LocalDateTime lastReportedAt;
}
