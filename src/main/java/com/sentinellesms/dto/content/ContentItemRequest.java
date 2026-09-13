package com.sentinellesms.dto.content;

import com.sentinellesms.entity.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContentItemRequest {

    @NotBlank
    private String key;

    @NotNull
    private ContentType type;

    /** Français par défaut — Bambara/audio gérés ailleurs. */
    private String language = "fr";

    @NotBlank
    private String title;

    @NotBlank
    private String body;

    private int sortOrder = 0;

    private boolean active = true;
}
