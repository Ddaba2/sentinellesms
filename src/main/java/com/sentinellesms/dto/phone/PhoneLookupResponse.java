package com.sentinellesms.dto.phone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneLookupResponse {

    private String normalizedPhone;
    private boolean reported;
    private String category;
    private String fraudTypes;
    private int reportCount;
    private int confidence;
    private LocalDateTime firstReportedAt;
    private LocalDateTime lastReportedAt;
    private List<PhoneReportHistoryItem> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhoneReportHistoryItem {
        private UUID reportId;
        private String category;
        private String riskLevel;
        private Integer riskScore;
        private LocalDateTime createdAt;
        private String status;
    }
}
