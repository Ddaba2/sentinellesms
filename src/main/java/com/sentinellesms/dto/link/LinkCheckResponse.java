package com.sentinellesms.dto.link;

import com.sentinellesms.entity.LinkVerdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkCheckResponse {

    private String url;
    private String normalizedUrl;
    private String domain;
    private LinkVerdict verdict;
    private int confidence;
    private int riskScore;
    private String riskBand;
    private List<String> signals;
    private String recommendation;
    private boolean knownInDatabase;
    private UUID reputationId;
}
