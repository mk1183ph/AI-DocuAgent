package com.docuagent.localapp.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record DocumentResponse(
        Long id,
        Long taskId,
        String generatedContent,
        Map<String, String> structuredContent,
        String filePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
