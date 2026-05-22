package com.docuagent.localapp.dto;

public record PlacementPreviewResponse(
        String semanticFieldKey,
        String displayName,
        String sourceLabel,
        String targetBlockType,
        Integer targetBlockOrder,
        Integer tableIndex,
        Integer rowIndex,
        Integer cellIndex,
        String targetText,
        String draftValue,
        String status
) {
}
