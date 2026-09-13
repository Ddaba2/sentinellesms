package com.sentinellesms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String riskLevel;

    /** Score de risque optionnel 0–100 remonté par le client. */
    private Integer riskScore;

    private String reasonCodes;

    private String patternHash;

    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType = ReportType.MESSAGE;

    /** Numéro normalisé ou URL normalisée (données minimisées du signalement). */
    @Column(length = 500)
    private String targetValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModerationStatus status = ModerationStatus.PENDING;

    /** Niveau de confiance attribué à la validation (0–100). */
    private Integer confidence;

    @Column(nullable = false)
    private boolean reviewed = false;

    private UUID reviewedBy;

    private String reviewNotes;

    private UUID mergedIntoId;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (reportType == null) {
            reportType = ReportType.MESSAGE;
        }
        if (status == null) {
            status = ModerationStatus.PENDING;
        }
    }
}
