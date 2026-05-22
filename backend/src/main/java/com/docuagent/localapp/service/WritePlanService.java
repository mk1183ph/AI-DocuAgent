package com.docuagent.localapp.service;

import com.docuagent.localapp.domain.Tab;
import com.docuagent.localapp.domain.Task;
import com.docuagent.localapp.domain.TemplateAnalysis;
import com.docuagent.localapp.dto.PlacementPreviewResponse;
import com.docuagent.localapp.dto.TemplateBlockResponse;
import com.docuagent.localapp.dto.TemplateLabelResponse;
import com.docuagent.localapp.dto.WritePlanResponse;
import com.docuagent.localapp.exception.BadRequestException;
import com.docuagent.localapp.exception.ResourceNotFoundException;
import com.docuagent.localapp.repository.TabRepository;
import com.docuagent.localapp.repository.TaskRepository;
import com.docuagent.localapp.repository.TemplateAnalysisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WritePlanService {

    private final PlacementPreviewService placementPreviewService;
    private final TaskRepository taskRepository;
    private final TabRepository tabRepository;
    private final TemplateAnalysisRepository templateAnalysisRepository;
    private final ObjectMapper objectMapper;

    public WritePlanService(
            PlacementPreviewService placementPreviewService,
            TaskRepository taskRepository,
            TabRepository tabRepository,
            TemplateAnalysisRepository templateAnalysisRepository,
            ObjectMapper objectMapper
    ) {
        this.placementPreviewService = placementPreviewService;
        this.taskRepository = taskRepository;
        this.tabRepository = tabRepository;
        this.templateAnalysisRepository = templateAnalysisRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<WritePlanResponse> plan(Long taskId) {
        RawTemplateStructure structure = loadStructure(taskId);
        return placementPreviewService.preview(taskId).stream()
                .map(row -> toWritePlan(row, structure))
                .toList();
    }

    private RawTemplateStructure loadStructure(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        Tab tab = tabRepository.findById(task.getTabId())
                .orElseThrow(() -> new ResourceNotFoundException("Tab not found: " + task.getTabId()));
        TemplateAnalysis latestAnalysis = templateAnalysisRepository.findFirstByTabIdOrderByUpdatedAtDesc(tab.getId())
                .orElseThrow(() -> new BadRequestException("양식 구조 분석을 먼저 완료해주세요"));

        try {
            return objectMapper.readValue(latestAnalysis.getRawStructureJson(), RawTemplateStructure.class);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("저장된 양식 분석 결과를 읽지 못했습니다");
        }
    }

    private WritePlanResponse toWritePlan(PlacementPreviewResponse row, RawTemplateStructure structure) {
        if (!"READY".equals(row.status())) {
            return withOperation(row, row.targetBlockOrder(), row.tableIndex(), row.rowIndex(), row.cellIndex(), null, row.status());
        }

        if ("PARAGRAPH".equals(row.targetBlockType())) {
            return withOperation(
                    row,
                    row.targetBlockOrder(),
                    null,
                    null,
                    null,
                    "INSERT_AFTER_PARAGRAPH",
                    "READY"
            );
        }

        if ("TABLE_CELL".equals(row.targetBlockType())) {
            if (row.tableIndex() == null || row.rowIndex() == null || row.cellIndex() == null) {
                return withOperation(
                        row,
                        row.targetBlockOrder(),
                        row.tableIndex(),
                        row.rowIndex(),
                        row.cellIndex(),
                        null,
                        "NEEDS_MANUAL_REVIEW"
                );
            }

            return withOperation(
                    row,
                    row.targetBlockOrder(),
                    row.tableIndex(),
                    row.rowIndex(),
                    row.cellIndex() + 1,
                    "WRITE_TO_ADJACENT_CELL",
                    "READY"
            );
        }

        return withOperation(row, row.targetBlockOrder(), row.tableIndex(), row.rowIndex(), row.cellIndex(), null, "NEEDS_MANUAL_REVIEW");
    }

    private TemplateBlockResponse findWritableCell(PlacementPreviewResponse row, RawTemplateStructure structure) {
        if (row.tableIndex() == null || row.rowIndex() == null || row.cellIndex() == null) {
            return null;
        }

        return structure.blocks().stream()
                .filter(block -> "TABLE_CELL".equals(block.type()))
                .filter(block -> row.tableIndex().equals(block.tableIndex()))
                .filter(block -> row.rowIndex().equals(block.rowIndex()))
                .filter(block -> block.cellIndex() != null && block.cellIndex() > row.cellIndex())
                .min(Comparator.comparing(TemplateBlockResponse::cellIndex))
                .orElse(null);
    }

    private WritePlanResponse withOperation(
            PlacementPreviewResponse row,
            Integer targetBlockOrder,
            Integer tableIndex,
            Integer rowIndex,
            Integer cellIndex,
            String operationType,
            String status
    ) {
        return new WritePlanResponse(
                row.semanticFieldKey(),
                row.displayName(),
                row.sourceLabel(),
                row.targetBlockType(),
                targetBlockOrder,
                tableIndex,
                rowIndex,
                cellIndex,
                operationType,
                row.draftValue(),
                status
        );
    }

    private record RawTemplateStructure(
            List<TemplateBlockResponse> blocks,
            List<TemplateLabelResponse> labels
    ) {
    }
}
