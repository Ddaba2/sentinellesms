package com.sentinellesms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "model_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String version;

    @Column(length = 2000)
    private String releaseNotes;

    @Column(nullable = false)
    private String downloadUrl;

    private String checksum;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
