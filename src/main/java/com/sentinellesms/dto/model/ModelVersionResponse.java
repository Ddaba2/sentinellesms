package com.sentinellesms.dto.model;

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
public class ModelVersionResponse {

    private UUID id;

    private String version;

    private String releaseNotes;

    private String downloadUrl;

    private String checksum;

    private boolean active;

    private LocalDateTime createdAt;
}
