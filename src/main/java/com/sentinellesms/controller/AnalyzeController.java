package com.sentinellesms.controller;

import com.sentinellesms.dto.analyze.AnalyzeRequest;
import com.sentinellesms.dto.analyze.AnalyzeResponse;
import com.sentinellesms.service.AnalyzeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor
public class AnalyzeController {

    private final AnalyzeService analyzeService;

    @PostMapping
    public AnalyzeResponse analyze(@Valid @RequestBody AnalyzeRequest request) {
        return analyzeService.analyze(request);
    }
}
