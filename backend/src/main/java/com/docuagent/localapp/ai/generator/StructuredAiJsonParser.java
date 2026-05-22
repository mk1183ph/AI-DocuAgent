package com.docuagent.localapp.ai.generator;

import com.docuagent.localapp.ai.AiGenerationField;
import com.docuagent.localapp.exception.AiProviderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class StructuredAiJsonParser {

    private static final String NEEDS_INPUT = "추가 입력 필요";
    private static final String NOT_PROVIDED = "미기재";
    private static final TypeReference<LinkedHashMap<String, Object>> JSON_OBJECT_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public StructuredAiJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, String> parse(String rawResponse, List<AiGenerationField> fields) {
        LinkedHashMap<String, Object> parsed;
        try {
            parsed = objectMapper.readValue(extractJsonObject(rawResponse), JSON_OBJECT_TYPE);
        } catch (JsonProcessingException exception) {
            throw malformedJson();
        }

        LinkedHashMap<String, String> structuredContent = new LinkedHashMap<>();
        for (AiGenerationField field : fields) {
            Object value = parsed.get(field.fieldKey());
            structuredContent.put(field.fieldKey(), normalizeFieldValue(value, field.requiredValue()));
        }

        return structuredContent;
    }

    private String extractJsonObject(String rawResponse) {
        String candidate = stripMarkdownFence(rawResponse == null ? "" : rawResponse.trim());
        int start = candidate.indexOf('{');
        if (start < 0) {
            throw malformedJson();
        }

        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int index = start; index < candidate.length(); index++) {
            char character = candidate.charAt(index);
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (character == '\\') {
                    escaping = true;
                } else if (character == '"') {
                    inString = false;
                }
                continue;
            }

            if (character == '"') {
                inString = true;
            } else if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return candidate.substring(start, index + 1);
                }
            }
        }

        throw malformedJson();
    }

    private String stripMarkdownFence(String value) {
        if (!value.startsWith("```")) {
            return value;
        }

        String withoutOpeningFence = value.replaceFirst("^```(?:json)?\\s*", "");
        return withoutOpeningFence.replaceFirst("\\s*```\\s*$", "").trim();
    }

    private String normalizeFieldValue(Object value, boolean required) {
        String missingValue = required ? NEEDS_INPUT : NOT_PROVIDED;
        if (value == null) {
            return missingValue;
        }
        if (value instanceof String textValue) {
            String trimmed = textValue.trim();
            return trimmed.isBlank() ? missingValue : trimmed;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return missingValue;
    }

    private AiProviderException malformedJson() {
        return new AiProviderException(HttpStatus.BAD_GATEWAY, "AI 응답을 JSON으로 해석하지 못했습니다.");
    }
}
