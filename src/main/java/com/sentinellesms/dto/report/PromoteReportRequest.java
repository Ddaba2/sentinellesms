package com.sentinellesms.dto.report;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromoteReportRequest {

    @NotBlank
    private String patternHash;

    @NotBlank
    private String label;

    @NotBlank
    private String keywords;

    private String description;
}
