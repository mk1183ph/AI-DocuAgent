package com.docuagent.localapp.dto;

public record TemplateFieldMappingUpdateRequest(
        Long id,

        String sourceLabel,

        String fieldKey,

        String semanticFieldKey,

        String displayName,

        String description,

        Boolean required,

        String mappingStatus,

        String confidenceLevel,

        String writingRule
) {
}
