package com.sentinellesms.repository;

import com.sentinellesms.entity.LinkReputation;
import com.sentinellesms.entity.LinkVerdict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkReputationRepository extends JpaRepository<LinkReputation, UUID> {

    Optional<LinkReputation> findByNormalizedUrl(String normalizedUrl);

    Optional<LinkReputation> findByDomainAndActiveTrue(String domain);

    List<LinkReputation> findByActiveTrueOrderByLastReportedAtDesc();

    long countByActiveTrue();

    long countByVerdictAndActiveTrue(LinkVerdict verdict);
}
