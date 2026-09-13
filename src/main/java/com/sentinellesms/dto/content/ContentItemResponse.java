package com.sentinellesms.dto.content;

import com.sentinellesms.entity.ContentType;
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
public class ContentItemResponse {

    private UUID id;
    private String key;
    private ContentType type;
    private String language;
    private String title;
    private String body;
    private int sortOrder;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
