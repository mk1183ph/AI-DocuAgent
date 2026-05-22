package com.docuagent.localapp.ai;

public record AiGenerationField(
        String sourceLabel,
        String fieldKey,
        String displayName,
        String description,
        Boolean required,
        String writingRule
) {

    public String semanticFieldKey() {
        return fieldKey;
    }

    public boolean requiredValue() {
        return Boolean.TRUE.equals(required);
    }
}
