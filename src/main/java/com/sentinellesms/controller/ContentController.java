package com.sentinellesms.controller;

import com.sentinellesms.dto.content.ContentItemRequest;
import com.sentinellesms.dto.content.ContentItemResponse;
import com.sentinellesms.entity.ContentType;
import com.sentinellesms.service.ContentService;
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
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping("/public")
    public List<ContentItemResponse> listPublic(@RequestParam(required = false, defaultValue = "fr") String language,
                                                @RequestParam(required = false) ContentType type) {
        return contentService.listPublic(language, type);
    }

    @GetMapping
    public List<ContentItemResponse> listAll(@RequestParam(required = false, defaultValue = "fr") String language) {
        return contentService.listAll(language);
    }

    @PostMapping
    public ResponseEntity<ContentItemResponse> create(@Valid @RequestBody ContentItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.create(request));
    }

    @PutMapping("/{id}")
    public ContentItemResponse update(@PathVariable UUID id, @Valid @RequestBody ContentItemRequest request) {
        return contentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        contentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
