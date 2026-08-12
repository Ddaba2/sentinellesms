package com.sentinellesms.controller;

import com.sentinellesms.dto.pattern.FraudPatternRequest;
import com.sentinellesms.dto.pattern.FraudPatternResponse;
import com.sentinellesms.service.PatternService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patterns")
@RequiredArgsConstructor
public class PatternController {

    private final PatternService patternService;

    @GetMapping("/sync")
    public List<FraudPatternResponse> sync(@RequestParam(required = false) String language) {
        return patternService.listActive(language);
    }

    @GetMapping
    public List<FraudPatternResponse> listAll() {
        return patternService.listAll();
    }

    @PostMapping
    public ResponseEntity<FraudPatternResponse> create(@Valid @RequestBody FraudPatternRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patternService.create(request));
    }

    @PutMapping("/{id}")
    public FraudPatternResponse update(@PathVariable UUID id, @Valid @RequestBody FraudPatternRequest request) {
        return patternService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        patternService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
