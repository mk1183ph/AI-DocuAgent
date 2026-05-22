package com.docuagent.localapp.dto;

import java.time.LocalDateTime;

public record TemplateFieldMappingResponse(
        Long id,
        String sourceLabel,
        String fieldKey,
        String semanticFieldKey,
        String displayName,
        String description,
        Boolean required,
        String mappingStatus,
        String confidenceLevel,
        String writingRule,
        Double confidence,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
