package com.docuagent.localapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record TabUpdateRequest(
        @NotBlank(message = "탭 이름을 입력하세요")
        @Size(max = 100, message = "탭 이름은 100자 이하여야 합니다")
        String name,

        @Size(max = 1000, message = "설명은 1000자 이하여야 합니다")
        String description,

        @Size(max = 4000, message = "기본 작성 규칙은 4000자 이하여야 합니다")
        String basePrompt,

        MultipartFile templateFile
) {
}
