package com.docuagent.localapp.dto;

import java.time.LocalDateTime;

public record TabResponse(
        Long id,
        String name,
        String description,
        String originalFileName,
        String templatePath,
        String basePrompt,
        LocalDateTime createdAt
) {
}
