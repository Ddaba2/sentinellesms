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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "link_reputations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkReputation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 500)
    private String normalizedUrl;

    @Column(nullable = false)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LinkVerdict verdict = LinkVerdict.SUSPECT;

    @Column(nullable = false)
    private int confidence = 0;

    @Column(nullable = false)
    private int reportCount = 0;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime firstReportedAt;

    private LocalDateTime lastReportedAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        firstReportedAt = now;
        lastReportedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
