package com.sentinellesms.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {

    private long totalReports;
    private long pendingReports;
    private long reportsLast7Days;
    private long activePatterns;
    private long trackedPhones;
    private long trackedLinks;
    private long enabledUsers;
    private Map<String, Long> byRiskLevel;
    private Map<String, Long> byCategory;
    private Map<String, Long> byLanguage;
    private Map<String, Long> byReportType;
    private Map<String, Long> byStatus;
    private Map<String, Long> linksByVerdict;
    private List<DailyCount> reportsTimeline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCount {
        private String date;
        private long count;
    }
}
