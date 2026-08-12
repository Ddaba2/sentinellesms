package com.sentinellesms.repository;

import com.sentinellesms.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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

    @Query("select r.patternHash, r.category, r.riskLevel, r.language, count(r), min(r.createdAt), max(r.createdAt) "
            + "from Report r "
            + "where r.reviewed = false and r.patternHash is not null and r.patternHash <> '' "
            + "group by r.patternHash, r.category, r.riskLevel, r.language "
            + "order by count(r) desc")
    List<Object[]> findTrendingClusters();

    List<Report> findByPatternHashAndReviewedFalse(String patternHash);
}
