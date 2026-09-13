package com.sentinellesms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "content_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"content_key", "language"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content_key", nullable = false)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType type;

    @Column(nullable = false, length = 10)
    private String language = "fr";

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 8000)
    private String body;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
