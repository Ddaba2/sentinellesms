package com.sentinellesms.service;

import com.sentinellesms.dto.statistics.StatisticsResponse;
import com.sentinellesms.entity.LinkVerdict;
import com.sentinellesms.entity.ModerationStatus;
import com.sentinellesms.repository.FraudPatternRepository;
import com.sentinellesms.repository.LinkReputationRepository;
import com.sentinellesms.repository.PhoneReputationRepository;
import com.sentinellesms.repository.ReportRepository;
import com.sentinellesms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ReportRepository reportRepository;
    private final FraudPatternRepository fraudPatternRepository;
    private final PhoneReputationRepository phoneReputationRepository;
    private final LinkReputationRepository linkReputationRepository;
    private final UserRepository userRepository;

    public StatisticsResponse buildOverview() {
        Map<String, Long> byRiskLevel = toMap(reportRepository.countByRiskLevel());
        Map<String, Long> byCategory = toMap(reportRepository.countByCategory());
        Map<String, Long> byLanguage = toMap(reportRepository.countByLanguage());
        Map<String, Long> byReportType = enumMap(reportRepository.countByReportType());
        Map<String, Long> byStatus = enumMap(reportRepository.countByStatus());

        Map<String, Long> linksByVerdict = new LinkedHashMap<>();
        for (LinkVerdict verdict : LinkVerdict.values()) {
            linksByVerdict.put(verdict.name(), linkReputationRepository.countByVerdictAndActiveTrue(verdict));
        }

        long total = reportRepository.count();
        LocalDateTime since = LocalDate.now().minusDays(6).atStartOfDay();
        List<StatisticsResponse.DailyCount> timeline = new ArrayList<>();
        for (Object[] row : reportRepository.countDailySince(since)) {
            timeline.add(StatisticsResponse.DailyCount.builder()
                    .date(String.valueOf(row[0]))
                    .count((Long) row[1])
                    .build());
        }

        return StatisticsResponse.builder()
                .totalReports(total)
                .pendingReports(reportRepository.countByStatus(ModerationStatus.PENDING))
                .reportsLast7Days(reportRepository.countByCreatedAtAfter(since))
                .activePatterns(fraudPatternRepository.countByActiveTrue())
                .trackedPhones(phoneReputationRepository.countByActiveTrue())
                .trackedLinks(linkReputationRepository.countByActiveTrue())
                .enabledUsers(userRepository.countByEnabledTrue())
                .byRiskLevel(byRiskLevel)
                .byCategory(byCategory)
                .byLanguage(byLanguage)
                .byReportType(byReportType)
                .byStatus(byStatus)
                .linksByVerdict(linksByVerdict)
                .reportsTimeline(timeline)
                .build();
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), (Long) row[1]);
        }
        return result;
    }

    private Map<String, Long> enumMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), (Long) row[1]);
        }
        return result;
    }
}
