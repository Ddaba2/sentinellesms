package com.sentinellesms.repository;

import com.sentinellesms.entity.ContentItem;
import com.sentinellesms.entity.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {

    Optional<ContentItem> findByKeyAndLanguage(String key, String language);

    List<ContentItem> findByActiveTrueAndLanguageOrderBySortOrderAsc(String language);

    List<ContentItem> findByActiveTrueAndTypeAndLanguageOrderBySortOrderAsc(ContentType type, String language);

    List<ContentItem> findByLanguageOrderBySortOrderAsc(String language);

    boolean existsByKeyAndLanguage(String key, String language);
}
