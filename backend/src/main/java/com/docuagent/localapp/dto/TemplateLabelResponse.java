package com.docuagent.localapp.dto;

public record TemplateLabelResponse(
        String text,
        int order,
        String type,
        Integer tableIndex,
        Integer rowIndex,
        Integer cellIndex
) {
}
