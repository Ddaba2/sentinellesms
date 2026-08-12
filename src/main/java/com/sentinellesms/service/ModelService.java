package com.sentinellesms.service;

import com.sentinellesms.dto.model.ModelSyncResponse;
import com.sentinellesms.dto.model.ModelVersionRequest;
import com.sentinellesms.dto.model.ModelVersionResponse;
import com.sentinellesms.entity.ModelVersion;
import com.sentinellesms.exception.ResourceNotFoundException;
import com.sentinellesms.repository.ModelVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModelService {

    private final ModelVersionRepository modelVersionRepository;
    private final PatternService patternService;

    public ModelSyncResponse getSyncPayload() {
        ModelVersion active = modelVersionRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .orElseThrow(() -> new ResourceNotFoundException("Aucune version de modèle disponible"));

        return ModelSyncResponse.builder()
                .modelVersion(toResponse(active))
                .patterns(patternService.listActive(null))
                .build();
    }

    public List<ModelVersionResponse> listVersions() {
        return modelVersionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ModelVersionResponse createVersion(ModelVersionRequest request) {
        modelVersionRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .ifPresent(previous -> {
                    previous.setActive(false);
                    modelVersionRepository.save(previous);
                });

        ModelVersion version = new ModelVersion();
        version.setVersion(request.getVersion());
        version.setReleaseNotes(request.getReleaseNotes());
        version.setDownloadUrl(request.getDownloadUrl());
        version.setChecksum(request.getChecksum());
        version.setActive(true);

        return toResponse(modelVersionRepository.save(version));
    }

    private ModelVersionResponse toResponse(ModelVersion version) {
        return ModelVersionResponse.builder()
                .id(version.getId())
                .version(version.getVersion())
                .releaseNotes(version.getReleaseNotes())
                .downloadUrl(version.getDownloadUrl())
                .checksum(version.getChecksum())
                .active(version.isActive())
                .createdAt(version.getCreatedAt())
                .build();
    }
}
