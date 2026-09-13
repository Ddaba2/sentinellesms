package com.sentinellesms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "fraud_patterns")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false, length = 1000)
    private String keywords;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String riskLevel;

    /** Contribution au score 0–100 lors de l'analyse. */
    @Column(nullable = false)
    private int riskScore = 50;

    private String description;

    /** Codes de signaux pédagogiques (urgence, pin, lien, etc.), séparés par des virgules. */
    private String signalCodes;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (riskScore < 0) {
            riskScore = 0;
        }
        if (riskScore > 100) {
            riskScore = 100;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
