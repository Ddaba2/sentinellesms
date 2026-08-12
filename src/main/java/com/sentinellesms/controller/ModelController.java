package com.sentinellesms.controller;

import com.sentinellesms.dto.model.ModelSyncResponse;
import com.sentinellesms.dto.model.ModelVersionRequest;
import com.sentinellesms.dto.model.ModelVersionResponse;
import com.sentinellesms.service.ModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @GetMapping("/latest")
    public ModelSyncResponse latest() {
        return modelService.getSyncPayload();
    }

    @GetMapping("/versions")
    public List<ModelVersionResponse> versions() {
        return modelService.listVersions();
    }

    @PostMapping("/versions")
    public ResponseEntity<ModelVersionResponse> createVersion(@Valid @RequestBody ModelVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(modelService.createVersion(request));
    }
}
