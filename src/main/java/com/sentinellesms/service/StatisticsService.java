package com.sentinellesms.service;

import com.sentinellesms.dto.statistics.StatisticsResponse;
import com.sentinellesms.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ReportRepository reportRepository;

    public StatisticsResponse buildOverview() {
        Map<String, Long> byRiskLevel = toMap(reportRepository.countByRiskLevel());
        Map<String, Long> byCategory = toMap(reportRepository.countByCategory());
        Map<String, Long> byLanguage = toMap(reportRepository.countByLanguage());

        long total = byRiskLevel.values().stream().mapToLong(Long::longValue).sum();

        return StatisticsResponse.builder()
                .totalReports(total)
                .byRiskLevel(byRiskLevel)
                .byCategory(byCategory)
                .byLanguage(byLanguage)
                .build();
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }
}
