package com.sentinellesms.repository;

import com.sentinellesms.entity.ModerationStatus;
import com.sentinellesms.entity.Report;
import com.sentinellesms.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    @Query("select r.riskLevel, count(r) from Report r group by r.riskLevel")
    List<Object[]> countByRiskLevel();

    @Query("select r.category, count(r) from Report r group by r.category")
    List<Object[]> countByCategory();

    @Query("select r.language, count(r) from Report r group by r.language")
    List<Object[]> countByLanguage();

    @Query("select r.reportType, count(r) from Report r group by r.reportType")
    List<Object[]> countByReportType();

    @Query("select r.status, count(r) from Report r group by r.status")
    List<Object[]> countByStatus();

    @Query("select r.patternHash, r.category, r.riskLevel, r.language, count(r), min(r.createdAt), max(r.createdAt) "
            + "from Report r "
            + "where r.reviewed = false and r.status = com.sentinellesms.entity.ModerationStatus.PENDING "
            + "and r.patternHash is not null and r.patternHash <> '' "
            + "group by r.patternHash, r.category, r.riskLevel, r.language "
            + "order by count(r) desc")
    List<Object[]> findTrendingClusters();

    List<Report> findByPatternHashAndReviewedFalse(String patternHash);

    List<Report> findByStatusOrderByCreatedAtAsc(ModerationStatus status);

    List<Report> findByStatusAndReportTypeOrderByCreatedAtAsc(ModerationStatus status, ReportType reportType);

    long countByStatus(ModerationStatus status);

    long countByCreatedAtAfter(LocalDateTime after);

    @Query(value = "select cast(r.created_at as date), count(*) from reports r "
            + "where r.created_at >= :since group by cast(r.created_at as date) order by 1",
            nativeQuery = true)
    List<Object[]> countDailySince(@Param("since") LocalDateTime since);

    List<Report> findByTargetValueAndReportTypeOrderByCreatedAtDesc(String targetValue, ReportType reportType);
}
