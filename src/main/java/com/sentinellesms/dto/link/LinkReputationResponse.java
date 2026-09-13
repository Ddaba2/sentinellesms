package com.sentinellesms.dto.link;

import com.sentinellesms.entity.LinkVerdict;
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
public class LinkReputationResponse {

    private UUID id;
    private String normalizedUrl;
    private String domain;
    private LinkVerdict verdict;
    private int confidence;
    private int reportCount;
    private String reason;
    private boolean active;
    private LocalDateTime firstReportedAt;
    private LocalDateTime lastReportedAt;
}
