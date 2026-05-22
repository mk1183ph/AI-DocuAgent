package com.docuagent.localapp.dto;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        Long tabId,
        String title,
        String userContext,
        String status,
        LocalDateTime createdAt
) {
}
