package com.docuagent.localapp.dto;

public record WritePlanResponse(
        String semanticFieldKey,
        String displayName,
        String sourceLabel,
        String targetBlockType,
        Integer targetBlockOrder,
        Integer tableIndex,
        Integer rowIndex,
        Integer cellIndex,
        String operationType,
        String value,
        String status
) {
}
