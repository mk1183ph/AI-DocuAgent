package com.docuagent.localapp.service;

import com.docuagent.localapp.domain.Document;
import com.docuagent.localapp.domain.Tab;
import com.docuagent.localapp.domain.Task;
import com.docuagent.localapp.dto.ReconstructedDocumentResponse;
import com.docuagent.localapp.dto.ReconstructionResultResponse;
import com.docuagent.localapp.dto.ReconstructionSummaryResponse;
import com.docuagent.localapp.dto.WritePlanResponse;
import com.docuagent.localapp.exception.BadRequestException;
import com.docuagent.localapp.exception.ResourceNotFoundException;
import com.docuagent.localapp.repository.DocumentRepository;
import com.docuagent.localapp.repository.TabRepository;
import com.docuagent.localapp.repository.TaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DocumentReconstructionService {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TaskRepository taskRepository;
    private final TabRepository tabRepository;
    private final DocumentRepository documentRepository;
    private final WritePlanService writePlanService;
    private final ObjectMapper objectMapper;
    private final Path generatedDirectory;

    public DocumentReconstructionService(
            TaskRepository taskRepository,
            TabRepository tabRepository,
            DocumentRepository documentRepository,
            WritePlanService writePlanService,
            ObjectMapper objectMapper,
            @Value("${app.storage.generated-dir}") String generatedDirectory
    ) {
        this.taskRepository = taskRepository;
        this.tabRepository = tabRepository;
        this.documentRepository = documentRepository;
        this.writePlanService = writePlanService;
        this.objectMapper = objectMapper;
        this.generatedDirectory = Path.of(generatedDirectory).normalize();
    }

    @Transactional
    public ReconstructedDocumentResponse reconstruct(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        Tab tab = tabRepository.findById(task.getTabId())
                .orElseThrow(() -> new ResourceNotFoundException("Tab not found: " + task.getTabId()));
        Document latestDraft = documentRepository.findFirstByTaskIdOrderByCreatedAtDesc(taskId)
                .orElseThrow(() -> new BadRequestException("저장된 초안이 없습니다."));
        Path templatePath = validateTemplate(tab);
        List<WritePlanResponse> writePlan = writePlanService.plan(taskId);

        try (InputStream inputStream = Files.newInputStream(templatePath);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            Map<Integer, XWPFParagraph> paragraphsByBlockOrder = indexTopLevelParagraphs(document);
            List<ReconstructionResultResponse> results = applyWritePlan(document, paragraphsByBlockOrder, writePlan);
            ReconstructionSummaryResponse pendingSummary = toSummary(
                    task.getId(),
                    latestDraft.getId(),
                    latestDraft.getFilePath(),
                    results
            );
            if (pendingSummary.writtenCount() == 0) {
                throw new BadRequestException("DOCX에 삽입할 수 있는 초안 필드가 없습니다.");
            }

            Files.createDirectories(generatedDirectory);
            String downloadFileName = safeFileName(task.getTitle()) + ".docx";
            Path generatedPath = generatedDirectory
                    .resolve(FILE_TIMESTAMP.format(LocalDateTime.now()) + "-" + UUID.randomUUID() + "-" + downloadFileName)
                    .normalize();
            try (OutputStream outputStream = Files.newOutputStream(generatedPath)) {
                document.write(outputStream);
            }

            ReconstructionSummaryResponse summary = toSummary(
                    task.getId(),
                    latestDraft.getId(),
                    generatedPath.toString(),
                    results
            );
            latestDraft.updateFilePath(generatedPath.toString());
            latestDraft.updateReconstructionSummary(writeSummary(summary));
            documentRepository.save(latestDraft);
            return new ReconstructedDocumentResponse(generatedPath, downloadFileName, summary);
        } catch (IOException exception) {
            throw new BadRequestException("DOCX 완성 문서를 생성하지 못했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public ReconstructionSummaryResponse findLatestSummary(Long taskId) {
        taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        return documentRepository.findFirstByTaskIdOrderByCreatedAtDesc(taskId)
                .map(document -> readSummary(taskId, document))
                .orElseGet(() -> ReconstructionSummaryResponse.empty(taskId));
    }

    private Path validateTemplate(Tab tab) {
        if (!StringUtils.hasText(tab.getTemplatePath())) {
            throw new BadRequestException("업로드된 DOCX 양식이 없습니다.");
        }

        Path templatePath = Path.of(tab.getTemplatePath()).normalize();
        if (!Files.exists(templatePath)) {
            throw new BadRequestException("업로드된 DOCX 양식 파일을 찾을 수 없습니다.");
        }

        return templatePath;
    }

    private List<ReconstructionResultResponse> applyWritePlan(
            XWPFDocument document,
            Map<Integer, XWPFParagraph> paragraphsByBlockOrder,
            List<WritePlanResponse> writePlan
    ) {
        return writePlan.stream()
                .map(operation -> applyOperation(document, paragraphsByBlockOrder, operation))
                .toList();
    }

    private ReconstructionResultResponse applyOperation(
            XWPFDocument document,
            Map<Integer, XWPFParagraph> paragraphsByBlockOrder,
            WritePlanResponse operation
    ) {
        if (operation == null) {
            return new ReconstructionResultResponse(null, null, null, "SKIPPED_UNSUPPORTED_OPERATION", "지원하지 않는 삽입 작업입니다.");
        }
        if (!hasUsefulValue(operation.value()) || "NO_DRAFT_VALUE".equals(operation.status())) {
            return result(operation, "SKIPPED_NO_VALUE", "삽입할 초안 내용이 없습니다.");
        }
        if ("NO_TARGET_BLOCK".equals(operation.status()) || hasNoTarget(operation)) {
            return result(operation, "SKIPPED_NO_TARGET", noTargetMessage(operation));
        }
        if (!"READY".equals(operation.status()) || !StringUtils.hasText(operation.operationType())) {
            return result(operation, "SKIPPED_UNSUPPORTED_OPERATION", "양식 구조상 자동 삽입을 지원하지 않는 위치입니다.");
        }

        try {
            boolean written = switch (operation.operationType()) {
                case "WRITE_TO_ADJACENT_CELL", "WRITE_TO_NEXT_CELL" -> writeTableCell(document, operation);
                case "INSERT_AFTER_PARAGRAPH" -> insertAfterParagraph(paragraphsByBlockOrder, operation);
                default -> false;
            };
            return written
                    ? result(operation, "WRITTEN", "DOCX 양식에 삽입했습니다.")
                    : result(operation, "SKIPPED_WRITE_FAILED", writeFailedMessage(operation));
        } catch (RuntimeException exception) {
            return result(operation, "SKIPPED_WRITE_FAILED", writeFailedMessage(operation));
        }
    }

    private String noTargetMessage(WritePlanResponse operation) {
        if (!StringUtils.hasText(operation.sourceLabel())) {
            return "초안 전용 필드라 자동 삽입되지 않았습니다.";
        }
        return "문서에서 연결된 위치를 찾지 못했습니다.";
    }

    private String writeFailedMessage(WritePlanResponse operation) {
        if ("TABLE_CELL".equals(operation.targetBlockType())) {
            return "대상 표 셀이 없거나 병합된 셀 구조라 자동 삽입하지 못했습니다.";
        }
        return "대상 위치에 내용을 삽입하지 못했습니다.";
    }

    private boolean hasNoTarget(WritePlanResponse operation) {
        if ("TABLE_CELL".equals(operation.targetBlockType())) {
            return operation.tableIndex() == null || operation.rowIndex() == null || operation.cellIndex() == null;
        }
        if ("PARAGRAPH".equals(operation.targetBlockType())) {
            return operation.targetBlockOrder() == null;
        }
        return true;
    }

    private boolean hasUsefulValue(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        String trimmed = value.trim();
        return !"추가 입력 필요".equals(trimmed)
                && !"미기재".equals(trimmed)
                && !"異붽? ?낅젰 ?꾩슂".equals(trimmed)
                && !"誘멸린??".equals(trimmed);
    }

    private boolean writeTableCell(XWPFDocument document, WritePlanResponse operation) {
        if (operation.tableIndex() == null || operation.rowIndex() == null || operation.cellIndex() == null) {
            return false;
        }

        List<XWPFTable> tables = document.getTables();
        if (operation.tableIndex() < 0 || operation.tableIndex() >= tables.size()) {
            return false;
        }

        XWPFTable table = tables.get(operation.tableIndex());
        XWPFTableRow row = table.getRow(operation.rowIndex());
        if (row == null) {
            return false;
        }

        XWPFTableCell cell = row.getCell(operation.cellIndex());
        if (cell == null) {
            return false;
        }

        replaceCellText(cell, operation.value());
        return true;
    }

    private void replaceCellText(XWPFTableCell cell, String value) {
        while (!cell.getParagraphs().isEmpty()) {
            cell.removeParagraph(0);
        }

        XWPFParagraph paragraph = cell.addParagraph();
        writeText(paragraph, value);
    }

    private boolean insertAfterParagraph(
            Map<Integer, XWPFParagraph> paragraphsByBlockOrder,
            WritePlanResponse operation
    ) {
        if (operation.targetBlockOrder() == null) {
            return false;
        }

        XWPFParagraph targetParagraph = paragraphsByBlockOrder.get(operation.targetBlockOrder());
        if (targetParagraph == null) {
            return false;
        }

        XmlCursor cursor = targetParagraph.getCTP().newCursor();
        try {
            cursor.toNextSibling();
            XWPFParagraph insertedParagraph = targetParagraph.getDocument().insertNewParagraph(cursor);
            if (insertedParagraph == null) {
                return false;
            }
            writeText(insertedParagraph, operation.value());
            return true;
        } finally {
            cursor.dispose();
        }
    }

    private void writeText(XWPFParagraph paragraph, String value) {
        String[] lines = value.split("\\R", -1);
        XWPFRun run = paragraph.createRun();
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                run.addBreak();
            }
            run.setText(lines[index]);
        }
    }

    private Map<Integer, XWPFParagraph> indexTopLevelParagraphs(XWPFDocument document) {
        Map<Integer, XWPFParagraph> paragraphsByBlockOrder = new LinkedHashMap<>();
        int blockOrder = 0;

        for (IBodyElement element : document.getBodyElements()) {
            if (element.getElementType() == BodyElementType.PARAGRAPH) {
                XWPFParagraph paragraph = (XWPFParagraph) element;
                if (StringUtils.hasText(normalizeText(paragraph.getText()))) {
                    blockOrder++;
                    paragraphsByBlockOrder.put(blockOrder, paragraph);
                }
                continue;
            }

            if (element.getElementType() == BodyElementType.TABLE) {
                blockOrder += countNonBlankTableCells((XWPFTable) element);
            }
        }

        return paragraphsByBlockOrder;
    }

    private int countNonBlankTableCells(XWPFTable table) {
        int count = 0;
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                if (StringUtils.hasText(normalizeText(cell.getText()))) {
                    count++;
                }
            }
        }
        return count;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safeFileName(String title) {
        String sanitized = StringUtils.hasText(title) ? title.trim() : "완성_문서";
        sanitized = sanitized.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        if (!StringUtils.hasText(sanitized)) {
            sanitized = "완성_문서";
        }
        return sanitized.length() > 80 ? sanitized.substring(0, 80) : sanitized;
    }

    private ReconstructionResultResponse result(WritePlanResponse operation, String status, String message) {
        return new ReconstructionResultResponse(
                operation.semanticFieldKey(),
                operation.displayName(),
                operation.sourceLabel(),
                status,
                message
        );
    }

    private ReconstructionSummaryResponse toSummary(
            Long taskId,
            Long documentId,
            String generatedFilePath,
            List<ReconstructionResultResponse> results
    ) {
        int writtenCount = countStatus(results, "WRITTEN");
        int totalOperations = results.size();
        return new ReconstructionSummaryResponse(
                taskId,
                documentId,
                generatedFilePath,
                totalOperations,
                writtenCount,
                totalOperations - writtenCount,
                true,
                results
        );
    }

    private int countStatus(List<ReconstructionResultResponse> results, String status) {
        return (int) results.stream()
                .filter(result -> status.equals(result.status()))
                .count();
    }

    private String writeSummary(ReconstructionSummaryResponse summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("문서 생성 결과를 저장하지 못했습니다.");
        }
    }

    private ReconstructionSummaryResponse readSummary(Long taskId, Document document) {
        if (!StringUtils.hasText(document.getReconstructionSummary())) {
            return ReconstructionSummaryResponse.empty(taskId);
        }

        try {
            return objectMapper.readValue(document.getReconstructionSummary(), ReconstructionSummaryResponse.class);
        } catch (JsonProcessingException exception) {
            return ReconstructionSummaryResponse.empty(taskId);
        }
    }
}
