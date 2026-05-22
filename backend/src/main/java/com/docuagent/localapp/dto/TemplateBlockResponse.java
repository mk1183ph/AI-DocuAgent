package com.docuagent.localapp.dto;

public record TemplateBlockResponse(
        String type,
        int order,
        String text,
        Integer tableIndex,
        Integer rowIndex,
        Integer cellIndex,
        boolean labelCandidate
) {
}
