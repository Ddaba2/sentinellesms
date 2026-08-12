package com.sentinellesms.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {

    private long totalReports;

    private Map<String, Long> byRiskLevel;

    private Map<String, Long> byCategory;

    private Map<String, Long> byLanguage;
}
