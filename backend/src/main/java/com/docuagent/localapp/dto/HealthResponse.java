package com.docuagent.localapp.dto;

import java.time.LocalDateTime;

public record HealthResponse(
        String status,
        String application,
        LocalDateTime checkedAt
) {
}
