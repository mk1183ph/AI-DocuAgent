package com.docuagent.localapp.dto;

import com.docuagent.localapp.ai.generator.AiProviderType;
import com.docuagent.localapp.ai.prompt.AiWritingMode;
import java.time.LocalDateTime;

public record SettingsResponse(
        AiProviderType aiProvider,
        AiWritingMode aiWritingMode,
        String ollamaBaseUrl,
        String ollamaModel,
        String geminiApiKey,
        String geminiModel,
        Integer requestTimeoutSeconds,
        LocalDateTime updatedAt
) {
}
