package com.docuagent.localapp.ai.prompt;

import com.docuagent.localapp.ai.AiGenerationField;
import com.docuagent.localapp.ai.AiGenerationRequest;
import com.docuagent.localapp.domain.Tab;
import com.docuagent.localapp.domain.Task;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AiPromptBuilder {

    public AiGenerationRequest build(Tab tab, Task task, List<AiGenerationField> fields, Integer inferenceStrength) {
        int resolvedStrength = inferenceStrength == null ? 55 : Math.max(0, Math.min(100, inferenceStrength));
        return new AiGenerationRequest(
                SafeDocumentSystemPrompt.content(),
                userPrompt(tab, task, fields, resolvedStrength),
                tab.getName(),
                tab.getDescription(),
                tab.getBasePrompt(),
                task.getTitle(),
                task.getUserContext(),
                fields
        );
    }

    private String userPrompt(Tab tab, Task task, List<AiGenerationField> fields, int inferenceStrength) {
        String fieldSchema = fields.stream()
                .map(this::fieldLine)
                .collect(Collectors.joining("\n"));

        String requiredKeys = fields.stream()
                .map(field -> "- \"" + field.fieldKey() + "\"")
                .distinct()
                .collect(Collectors.joining("\n"));

        return """
                You must transform the user's factual context into structured JSON for a DOCX template.

                Non-negotiable safety rules:
                - Never invent activities, child reactions, counseling content, names, dates, organizations, participants, or outcomes.
                - Never present a guess as a fact.
                - Never write diagnoses, medical/legal speculation, or personal history that the user did not provide.
                - If a field has no grounded support, write "추가 입력 필요" or "미기재".

                Grounded inference rule:
                - You may make reasonable educational, counseling, or administrative interpretations only when directly supported by supplied facts.
                - Example allowed: if the user says a child had difficulty joining peers, you may write that peer interaction support appears needed.
                - Example forbidden: do not claim developmental delay, family causes, new events, new reactions, new dates, or outcomes not provided.

                Template context:
                - Tab name: %s
                - Description: %s
                - Base writing rules: %s

                Dynamic document field schema:
                %s

                Required JSON keys:
                %s

                Inference strength:
                %d / 100

                Inference guidance:
                %s

                Task title:
                %s

                User factual context:
                %s

                Response contract:
                - Return a single JSON object only.
                - Do not wrap the JSON in markdown code fences.
                - Do not include explanations, comments, prose, headings, or text before or after the JSON.
                - Use only the fieldKey values listed above as JSON keys.
                - Do not add, rename, remove, or invent JSON keys.
                - Every listed JSON key must exist exactly once, even when information is missing.
                - Each value must be a string.
                - Values must be grounded in the user factual context or a reasonable interpretation of those facts under the inference strength.
                - Missing facts must be represented as "추가 입력 필요" or "미기재".
                """.formatted(
                valueOrBlank(tab.getName()),
                valueOrBlank(tab.getDescription()),
                valueOrBlank(tab.getBasePrompt()),
                fieldSchema,
                requiredKeys,
                inferenceStrength,
                inferenceGuidance(inferenceStrength),
                valueOrBlank(task.getTitle()),
                valueOrBlank(task.getUserContext())
        );
    }

    private String fieldLine(AiGenerationField field) {
        return "- fieldKey: \"%s\", displayName: \"%s\", sourceLabel: \"%s\", required: %s, description: \"%s\", writingRule: \"%s\""
                .formatted(
                        valueOrBlank(field.fieldKey()),
                        valueOrBlank(field.displayName()),
                        valueOrBlank(field.sourceLabel()),
                        field.requiredValue(),
                        valueOrBlank(field.description()),
                        valueOrBlank(field.writingRule())
                );
    }

    private String inferenceGuidance(int inferenceStrength) {
        if (inferenceStrength <= 34) {
            return """
                    - Conservative mode: write only what is directly supported by explicit user facts.
                    - Use "추가 입력 필요" or "미기재" when a field is not directly supported.
                    - Keep wording concise and avoid interpretation unless the user stated it clearly.
                    """;
        }
        if (inferenceStrength <= 74) {
            return """
                    - Balanced mode: use explicit user facts as the source of truth.
                    - Make grounded, reasonable interpretations from supplied facts when helpful.
                    - Keep the output professional and useful, but do not add unsupported details.
                    """;
        }
        return """
                - Aggressive grounded expansion mode: create a richer professional draft by expanding and organizing supplied facts.
                - Contextual interpretation is allowed only when clearly grounded in the input.
                - Mark uncertain or missing details as "추가 입력 필요" or "미기재"; never present guesses as facts.
                """;
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}
