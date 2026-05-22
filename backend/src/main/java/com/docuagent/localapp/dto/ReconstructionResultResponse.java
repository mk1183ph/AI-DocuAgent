package com.docuagent.localapp.dto;

public record ReconstructionResultResponse(
        String semanticFieldKey,
        String displayName,
        String sourceLabel,
        String status,
        String message
) {
}
