package com.docuagent.localapp.ai.generator;

public record AiGenerationOptions(
        String ollamaBaseUrl,
        String ollamaModel,
        String geminiApiKey,
        String geminiModel,
        int requestTimeoutSeconds
) {
}
