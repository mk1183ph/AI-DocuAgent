package com.docuagent.localapp.service;

import com.docuagent.localapp.domain.Document;
import com.docuagent.localapp.domain.Tab;
import com.docuagent.localapp.domain.Task;
import com.docuagent.localapp.domain.TemplateAnalysis;
import com.docuagent.localapp.domain.TemplateFieldMapping;
import com.docuagent.localapp.dto.PlacementPreviewResponse;
import com.docuagent.localapp.dto.TemplateBlockResponse;
import com.docuagent.localapp.dto.TemplateLabelResponse;
import com.docuagent.localapp.exception.BadRequestException;
import com.docuagent.localapp.exception.ResourceNotFoundException;
import com.docuagent.localapp.repository.DocumentRepository;
import com.docuagent.localapp.repository.TabRepository;
import com.docuagent.localapp.repository.TaskRepository;
import com.docuagent.localapp.repository.TemplateAnalysisRepository;
import com.docuagent.localapp.repository.TemplateFieldMappingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PlacementPreviewService {

    private static final TypeReference<Map<String, String>> STRUCTURED_CONTENT_TYPE = new TypeReference<>() {
    };

    private final TaskRepository taskRepository;
    private final TabRepository tabRepository;
    private final DocumentRepository documentRepository;
    private final TemplateAnalysisRepository templateAnalysisRepository;
    private final TemplateFieldMappingRepository templateFieldMappingRepository;
    private final ObjectMapper objectMapper;

    public PlacementPreviewService(
            TaskRepository taskRepository,
            TabRepository tabRepository,
            DocumentRepository documentRepository,
            TemplateAnalysisRepository templateAnalysisRepository,
            TemplateFieldMappingRepository templateFieldMappingRepository,
            ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.tabRepository = tabRepository;
        this.documentRepository = documentRepository;
        this.templateAnalysisRepository = templateAnalysisRepository;
        this.templateFieldMappingRepository = templateFieldMappingRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PlacementPreviewResponse> preview(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        Tab tab = tabRepository.findById(task.getTabId())
                .orElseThrow(() -> new ResourceNotFoundException("Tab not found: " + task.getTabId()));
        Document latestDraft = documentRepository.findFirstByTaskIdOrderByCreatedAtDesc(taskId)
                .orElseThrow(() -> new BadRequestException("저장된 초안이 없습니다."));
        TemplateAnalysis latestAnalysis = templateAnalysisRepository.findFirstByTabIdOrderByUpdatedAtDesc(tab.getId())
                .orElseThrow(() -> new BadRequestException("양식 구조 분석을 먼저 완료해주세요."));
        List<TemplateFieldMapping> mappings = templateFieldMappingRepository.findAllByTabIdOrderByCreatedAtAsc(tab.getId())
                .stream()
                .filter(mapping -> StringUtils.hasText(mapping.getFieldKey()))
                .toList();
        if (mappings.isEmpty()) {
            throw new BadRequestException("양식 필드를 먼저 확인해주세요.");
        }

        RawTemplateStructure structure = readRawStructure(latestAnalysis.getRawStructureJson());
        Map<String, String> draftValues = readStructuredContent(latestDraft.getGeneratedContent());

        return mappings.stream()
                .map(mapping -> toPreviewRow(mapping, structure, draftValues))
                .toList();
    }

    private PlacementPreviewResponse toPreviewRow(
            TemplateFieldMapping mapping,
            RawTemplateStructure structure,
            Map<String, String> draftValues
    ) {
        MatchedBlock matchedBlock = findTargetBlock(mapping.getSourceLabel(), structure);
        String draftValue = draftValues.getOrDefault(
                mapping.getFieldKey(),
                draftValues.getOrDefault(mapping.getSemanticFieldKey(), "")
        );
        String status = resolveStatus(matchedBlock, draftValue);

        return new PlacementPreviewResponse(
                mapping.getFieldKey(),
                mapping.getDisplayName(),
                mapping.getSourceLabel(),
                matchedBlock == null ? null : matchedBlock.type(),
                matchedBlock == null ? null : matchedBlock.order(),
                matchedBlock == null ? null : matchedBlock.tableIndex(),
                matchedBlock == null ? null : matchedBlock.rowIndex(),
                matchedBlock == null ? null : matchedBlock.cellIndex(),
                matchedBlock == null ? null : matchedBlock.text(),
                draftValue,
                status
        );
    }

    private MatchedBlock findTargetBlock(String sourceLabel, RawTemplateStructure structure) {
        String normalizedSource = normalize(sourceLabel);
        if (!StringUtils.hasText(normalizedSource)) {
            return null;
        }

        for (TemplateLabelResponse label : structure.labels()) {
            if (normalizedSource.equals(normalize(label.text()))) {
                return new MatchedBlock(
                        label.type(),
                        label.order(),
                        label.tableIndex(),
                        label.rowIndex(),
                        label.cellIndex(),
                        label.text()
                );
            }
        }

        for (TemplateBlockResponse block : structure.blocks()) {
            if (normalizedSource.equals(normalize(block.text()))) {
                return new MatchedBlock(
                        block.type(),
                        block.order(),
                        block.tableIndex(),
                        block.rowIndex(),
                        block.cellIndex(),
                        block.text()
                );
            }
        }

        return null;
    }

    private String resolveStatus(MatchedBlock matchedBlock, String draftValue) {
        if (matchedBlock == null) {
            return "NO_TARGET_BLOCK";
        }

        if (!StringUtils.hasText(draftValue)
                || "추가 입력 필요".equals(draftValue.trim())
                || "미기재".equals(draftValue.trim())) {
            return "NO_DRAFT_VALUE";
        }

        return "READY";
    }

    private RawTemplateStructure readRawStructure(String rawStructureJson) {
        try {
            return objectMapper.readValue(rawStructureJson, RawTemplateStructure.class);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("저장된 양식 분석 결과를 읽지 못했습니다.");
        }
    }

    private Map<String, String> readStructuredContent(String generatedContent) {
        try {
            return objectMapper.readValue(generatedContent, STRUCTURED_CONTENT_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("구조화된 초안을 먼저 생성해주세요.");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private record RawTemplateStructure(
            List<TemplateBlockResponse> blocks,
            List<TemplateLabelResponse> labels
    ) {
    }

    private record MatchedBlock(
            String type,
            Integer order,
            Integer tableIndex,
            Integer rowIndex,
            Integer cellIndex,
            String text
    ) {
    }
}
