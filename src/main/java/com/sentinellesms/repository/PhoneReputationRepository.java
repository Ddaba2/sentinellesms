package com.sentinellesms.repository;

import com.sentinellesms.entity.PhoneReputation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhoneReputationRepository extends JpaRepository<PhoneReputation, UUID> {

    Optional<PhoneReputation> findByNormalizedPhone(String normalizedPhone);

    List<PhoneReputation> findByActiveTrueOrderByLastReportedAtDesc();

    long countByActiveTrue();
}
