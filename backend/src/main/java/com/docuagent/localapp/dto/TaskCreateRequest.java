package com.docuagent.localapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskCreateRequest(
        @NotBlank(message = "작업명을 입력하세요")
        @Size(max = 200, message = "작업명은 200자 이하여야 합니다")
        String title,

        @NotBlank(message = "작성 참고 내용을 입력하세요")
        @Size(max = 8000, message = "작성 참고 내용은 8000자 이하여야 합니다")
        String userContext
) {
}
