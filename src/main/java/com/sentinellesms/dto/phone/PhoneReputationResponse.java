package com.sentinellesms.dto.phone;

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
public class PhoneReputationResponse {

    private UUID id;
    private String normalizedPhone;
    private String category;
    private String fraudTypes;
    private int reportCount;
    private int confidence;
    private boolean active;
    private LocalDateTime firstReportedAt;
    private LocalDateTime lastReportedAt;
}
