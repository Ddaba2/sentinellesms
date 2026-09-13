package com.sentinellesms.service;

import com.sentinellesms.dto.content.ContentItemRequest;
import com.sentinellesms.dto.content.ContentItemResponse;
import com.sentinellesms.entity.ContentItem;
import com.sentinellesms.entity.ContentType;
import com.sentinellesms.exception.BadRequestException;
import com.sentinellesms.exception.ResourceNotFoundException;
import com.sentinellesms.repository.ContentItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentItemRepository contentItemRepository;
    private final AuditService auditService;

    public List<ContentItemResponse> listPublic(String language, ContentType type) {
        String lang = language == null || language.isBlank() ? "fr" : language;
        List<ContentItem> items = type == null
                ? contentItemRepository.findByActiveTrueAndLanguageOrderBySortOrderAsc(lang)
                : contentItemRepository.findByActiveTrueAndTypeAndLanguageOrderBySortOrderAsc(type, lang);
        return items.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ContentItemResponse> listAll(String language) {
        String lang = language == null || language.isBlank() ? "fr" : language;
        return contentItemRepository.findByLanguageOrderBySortOrderAsc(lang).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContentItemResponse create(ContentItemRequest request) {
        String language = request.getLanguage() == null || request.getLanguage().isBlank()
                ? "fr" : request.getLanguage();
        if (contentItemRepository.existsByKeyAndLanguage(request.getKey(), language)) {
            throw new BadRequestException("Contenu déjà existant pour la clé/langue: "
                    + request.getKey() + "/" + language);
        }
        ContentItem item = new ContentItem();
        apply(item, request, language);
        item = contentItemRepository.save(item);
        auditService.log("CONTENT_CREATE", "ContentItem", item.getId().toString(), item.getKey());
        return toResponse(item);
    }

    @Transactional
    public ContentItemResponse update(UUID id, ContentItemRequest request) {
        ContentItem item = contentItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenu introuvable: " + id));
        String language = request.getLanguage() == null || request.getLanguage().isBlank()
                ? item.getLanguage() : request.getLanguage();
        contentItemRepository.findByKeyAndLanguage(request.getKey(), language)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Contenu déjà existant pour la clé/langue");
                });
        apply(item, request, language);
        item = contentItemRepository.save(item);
        auditService.log("CONTENT_UPDATE", "ContentItem", id.toString(), item.getKey());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        if (!contentItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Contenu introuvable: " + id);
        }
        contentItemRepository.deleteById(id);
        auditService.log("CONTENT_DELETE", "ContentItem", id.toString(), null);
    }

    private void apply(ContentItem item, ContentItemRequest request, String language) {
        item.setKey(request.getKey());
        item.setType(request.getType());
        item.setLanguage(language);
        item.setTitle(request.getTitle());
        item.setBody(request.getBody());
        item.setSortOrder(request.getSortOrder());
        item.setActive(request.isActive());
    }

    private ContentItemResponse toResponse(ContentItem item) {
        return ContentItemResponse.builder()
                .id(item.getId())
                .key(item.getKey())
                .type(item.getType())
                .language(item.getLanguage())
                .title(item.getTitle())
                .body(item.getBody())
                .sortOrder(item.getSortOrder())
                .active(item.isActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
