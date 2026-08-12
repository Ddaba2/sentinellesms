package com.sentinellesms.dto.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModelVersionRequest {

    @NotBlank
    private String version;

    private String releaseNotes;

    @NotBlank
    private String downloadUrl;

    private String checksum;
}
