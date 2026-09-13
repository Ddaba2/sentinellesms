package com.sentinellesms.dto.link;

import com.sentinellesms.entity.LinkVerdict;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LinkReputationRequest {

    @NotBlank
    private String url;

    @NotNull
    private LinkVerdict verdict;

    @Min(0)
    @Max(100)
    private int confidence = 50;

    private String reason;

    private boolean active = true;
}
