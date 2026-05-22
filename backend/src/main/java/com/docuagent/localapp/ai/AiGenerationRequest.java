package com.docuagent.localapp.ai;

import java.util.List;

public record AiGenerationRequest(
        String systemPrompt,
        String userPrompt,
        String tabName,
        String tabDescription,
        String basePrompt,
        String taskTitle,
        String userContext,
        List<AiGenerationField> fields
) {
}
