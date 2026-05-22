package com.docuagent.localapp.service;

import com.docuagent.localapp.domain.Document;
import com.docuagent.localapp.dto.DocumentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DocumentResponseMapper {

    private static final TypeReference<LinkedHashMap<String, String>> STRUCTURED_CONTENT_TYPE =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;

    public DocumentResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTaskId(),
                document.getGeneratedContent(),
                readStructuredContent(document.getGeneratedContent()),
                document.getFilePath(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private Map<String, String> readStructuredContent(String generatedContent) {
        if (!StringUtils.hasText(generatedContent)) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(generatedContent, STRUCTURED_CONTENT_TYPE);
        } catch (JsonProcessingException exception) {
            return Map.of("content", generatedContent);
        }
    }
}
