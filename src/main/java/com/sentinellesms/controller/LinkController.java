package com.sentinellesms.controller;

import com.sentinellesms.dto.link.LinkCheckRequest;
import com.sentinellesms.dto.link.LinkCheckResponse;
import com.sentinellesms.dto.link.LinkReputationRequest;
import com.sentinellesms.dto.link.LinkReputationResponse;
import com.sentinellesms.service.LinkService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @PostMapping("/check")
    public LinkCheckResponse check(@Valid @RequestBody LinkCheckRequest request) {
        return linkService.check(request.getUrl());
    }

    @GetMapping
    public List<LinkReputationResponse> list() {
        return linkService.listAll();
    }

    @PostMapping
    public ResponseEntity<LinkReputationResponse> create(@Valid @RequestBody LinkReputationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(linkService.upsert(request));
    }

    @PutMapping
    public LinkReputationResponse update(@Valid @RequestBody LinkReputationRequest request) {
        return linkService.upsert(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        linkService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
