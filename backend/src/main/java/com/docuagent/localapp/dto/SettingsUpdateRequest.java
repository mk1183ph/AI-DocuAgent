package com.docuagent.localapp.dto;

import com.docuagent.localapp.ai.generator.AiProviderType;
import com.docuagent.localapp.ai.prompt.AiWritingMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SettingsUpdateRequest(
        @NotNull(message = "AI 제공자를 선택해주세요.")
        AiProviderType aiProvider,

        AiWritingMode aiWritingMode,

        String ollamaBaseUrl,

        String ollamaModel,

        String geminiApiKey,

        String geminiModel,

        @NotNull(message = "요청 제한 시간을 입력해주세요.")
        @Min(value = 5, message = "요청 제한 시간은 5초 이상이어야 합니다.")
        @Max(value = 600, message = "요청 제한 시간은 600초 이하여야 합니다.")
        Integer requestTimeoutSeconds
) {
}
