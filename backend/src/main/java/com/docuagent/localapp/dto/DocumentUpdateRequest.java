package com.docuagent.localapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentUpdateRequest(
        @NotBlank(message = "초안 내용을 입력하세요")
        @Size(max = 12000, message = "초안 내용은 12000자 이하여야 합니다")
        String generatedContent
) {
}
