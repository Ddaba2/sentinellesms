package com.sentinellesms.dto.report;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ModerateReportRequest {

    @Min(0)
    @Max(100)
    private Integer confidence;

    private String reviewNotes;

    /** Pour une fusion : id du signalement (ou réputation) cible. */
    private java.util.UUID mergeIntoId;
}
