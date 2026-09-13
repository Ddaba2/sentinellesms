package com.sentinellesms.dto.link;

import com.sentinellesms.entity.LinkVerdict;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkCheckRequest {

    @NotBlank
    private String url;
}
