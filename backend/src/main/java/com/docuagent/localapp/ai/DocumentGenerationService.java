package com.docuagent.localapp.ai;

import com.docuagent.localapp.ai.generator.AiGenerationClient;
import com.docuagent.localapp.ai.generator.AiGenerationClientResolver;
import com.docuagent.localapp.ai.generator.AiGenerationOptions;
import com.docuagent.localapp.ai.prompt.AiPromptBuilder;
import com.docuagent.localapp.domain.Document;
import com.docuagent.localapp.domain.Settings;
import com.docuagent.localapp.domain.Tab;
import com.docuagent.localapp.domain.Task;
import com.docuagent.localapp.domain.TemplateFieldMapping;
import com.docuagent.localapp.dto.DocumentResponse;
import com.docuagent.localapp.exception.BadRequestException;
import com.docuagent.localapp.exception.ResourceNotFoundException;
import com.docuagent.localapp.repository.DocumentRepository;
import com.docuagent.localapp.repository.TabRepository;
import com.docuagent.localapp.repository.TaskRepository;
import com.docuagent.localapp.repository.TemplateFieldMappingRepository;
import com.docuagent.localapp.service.DocumentResponseMapper;
import com.docuagent.localapp.service.SettingsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DocumentGenerationService {

    private final TaskRepository taskRepository;
    private final TabRepository tabRepository;
    private final DocumentRepository documentRepository;
    private final TemplateFieldMappingRepository templateFieldMappingRepository;
    private final AiPromptBuilder promptBuilder;
    private final AiGenerationClientResolver generationClientResolver;
    private final SettingsService settingsService;
    private final ObjectMapper objectMapper;
    private final DocumentResponseMapper documentResponseMapper;

    public DocumentGenerationService(
            TaskRepository taskRepository,
            TabRepository tabRepository,
            DocumentRepository documentRepository,
            TemplateFieldMappingRepository templateFieldMappingRepository,
            AiPromptBuilder promptBuilder,
            AiGenerationClientResolver generationClientResolver,
            SettingsService settingsService,
            ObjectMapper objectMapper,
            DocumentResponseMapper documentResponseMapper
    ) {
        this.taskRepository = taskRepository;
        this.tabRepository = tabRepository;
        this.documentRepository = documentRepository;
        this.templateFieldMappingRepository = templateFieldMappingRepository;
        this.promptBuilder = promptBuilder;
        this.generationClientResolver = generationClientResolver;
        this.settingsService = settingsService;
        this.objectMapper = objectMapper;
        this.documentResponseMapper = documentResponseMapper;
    }

    @Transactional
    public DocumentResponse generateDraft(Long taskId, Integer inferenceStrength) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        Tab tab = tabRepository.findById(task.getTabId())
                .orElseThrow(() -> new ResourceNotFoundException("Tab not found: " + task.getTabId()));

        List<AiGenerationField> fields = templateFieldMappingRepository.findAllByTabIdOrderByCreatedAtAsc(tab.getId())
                .stream()
                .filter(mapping -> StringUtils.hasText(mapping.getFieldKey()))
                .map(this::toGenerationField)
                .toList();
        if (fields.isEmpty()) {
            throw new BadRequestException("양식 필드를 먼저 확인해주세요.");
        }

        Settings settings = settingsService.getSettingsEntity();
        AiGenerationOptions options = settingsService.toGenerationOptions(settings);
        AiGenerationClient generationClient = generationClientResolver.resolve(settings.getAiProvider());
        AiGenerationRequest request = promptBuilder.build(tab, task, fields, resolveInferenceStrength(inferenceStrength, settings));
        AiGenerationResult result = generationClient.generate(request, options);
        Document document = documentRepository.save(new Document(task.getId(), toJson(result, fields), null));

        return documentResponseMapper.toResponse(document);
    }

    private AiGenerationField toGenerationField(TemplateFieldMapping mapping) {
        return new AiGenerationField(
                mapping.getSourceLabel(),
                mapping.getFieldKey(),
                mapping.getDisplayName(),
                mapping.getDescription(),
                mapping.getRequired(),
                mapping.getWritingRule()
        );
    }

    private String toJson(AiGenerationResult result, List<AiGenerationField> fields) {
        try {
            return objectMapper.writeValueAsString(orderedContent(result.structuredContent(), fields));
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("구조화된 초안을 저장하지 못했습니다.");
        }
    }

    private Map<String, String> orderedContent(Map<String, String> structuredContent, List<AiGenerationField> fields) {
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        for (AiGenerationField field : fields) {
            String missingValue = field.requiredValue() ? "추가 입력 필요" : "미기재";
            ordered.put(field.fieldKey(), structuredContent.getOrDefault(field.fieldKey(), missingValue));
        }
        return ordered;
    }

    private Integer resolveInferenceStrength(Integer requestedStrength, Settings settings) {
        if (requestedStrength != null) {
            return Math.max(0, Math.min(100, requestedStrength));
        }

        return switch (settings.getAiWritingMode()) {
            case CONSERVATIVE -> 20;
            case BALANCED -> 55;
            case AGGRESSIVE -> 85;
        };
    }
}
