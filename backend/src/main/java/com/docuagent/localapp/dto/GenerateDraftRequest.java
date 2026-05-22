package com.docuagent.localapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GenerateDraftRequest(
        @Min(value = 0, message = "AI 추론 강도는 0 이상이어야 합니다.")
        @Max(value = 100, message = "AI 추론 강도는 100 이하여야 합니다.")
        Integer inferenceStrength
) {
}
