package com.docuagent.localapp.template.analysis;

import com.docuagent.localapp.domain.ConfidenceLevel;
import com.docuagent.localapp.domain.MappingStatus;
import com.docuagent.localapp.domain.Tab;
import com.docuagent.localapp.domain.TemplateAnalysis;
import com.docuagent.localapp.domain.TemplateFieldMapping;
import com.docuagent.localapp.dto.TemplateAnalysisResponse;
import com.docuagent.localapp.dto.TemplateBlockResponse;
import com.docuagent.localapp.dto.TemplateFieldMappingResponse;
import com.docuagent.localapp.dto.TemplateFieldMappingUpdateRequest;
import com.docuagent.localapp.dto.TemplateLabelResponse;
import com.docuagent.localapp.exception.BadRequestException;
import com.docuagent.localapp.exception.ResourceNotFoundException;
import com.docuagent.localapp.repository.TabRepository;
import com.docuagent.localapp.repository.TemplateAnalysisRepository;
import com.docuagent.localapp.repository.TemplateFieldMappingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TemplateAnalysisService {

    private static final int MAX_LABEL_LENGTH = 30;
    private static final Set<String> KNOWN_LABELS = Set.of(
            "활동 목표", "준비물", "활동 내용", "유아 반응", "특이사항",
            "상담 내용", "보호자 의견", "교사 의견", "관찰", "분석", "지원 계획"
    );

    private final TabRepository tabRepository;
    private final TemplateAnalysisRepository templateAnalysisRepository;
    private final TemplateFieldMappingRepository templateFieldMappingRepository;
    private final ObjectMapper objectMapper;

    public TemplateAnalysisService(
            TabRepository tabRepository,
            TemplateAnalysisRepository templateAnalysisRepository,
            TemplateFieldMappingRepository templateFieldMappingRepository,
            ObjectMapper objectMapper
    ) {
        this.tabRepository = tabRepository;
        this.templateAnalysisRepository = templateAnalysisRepository;
        this.templateFieldMappingRepository = templateFieldMappingRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TemplateAnalysisResponse analyze(Long tabId) {
        Tab tab = findTab(tabId);
        validateTemplate(tab);

        try (InputStream inputStream = Files.newInputStream(Path.of(tab.getTemplatePath()));
             XWPFDocument document = new XWPFDocument(inputStream)) {
            List<TemplateBlockResponse> blocks = extractBlocks(document);
            List<TemplateLabelResponse> labels = blocks.stream()
                    .filter(TemplateBlockResponse::labelCandidate)
                    .map(this::toLabel)
                    .toList();
            TemplateAnalysis analysis = saveAnalysis(tab.getId(), blocks, labels);
            List<TemplateFieldMappingResponse> mappings = saveAutoMappings(tab.getId(), labels);

            return toResponse(tab, analysis, blocks, labels, mappings);
        } catch (IOException exception) {
            throw new BadRequestException("DOCX 양식을 분석하지 못했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public TemplateAnalysisResponse findLatestAnalysis(Long tabId) {
        Tab tab = findTab(tabId);
        TemplateAnalysis analysis = templateAnalysisRepository.findFirstByTabIdOrderByUpdatedAtDesc(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("저장된 양식 분석 결과가 없습니다."));
        RawTemplateStructure rawStructure = readRawStructure(analysis.getRawStructureJson());
        List<TemplateFieldMappingResponse> mappings = findMappings(tabId);

        return toResponse(tab, analysis, rawStructure.blocks(), rawStructure.labels(), mappings);
    }

    @Transactional(readOnly = true)
    public List<TemplateFieldMappingResponse> findMappings(Long tabId) {
        findTab(tabId);
        return templateFieldMappingRepository.findAllByTabIdOrderByCreatedAtAsc(tabId).stream()
                .sorted(Comparator.comparing(TemplateFieldMapping::getFieldKey, Comparator.nullsLast(String::compareTo)))
                .map(this::toMappingResponse)
                .toList();
    }

    @Transactional
    public List<TemplateFieldMappingResponse> updateMappings(Long tabId, List<TemplateFieldMappingUpdateRequest> requests) {
        findTab(tabId);
        if (requests == null) {
            throw new BadRequestException("문서 필드 요청이 비어 있습니다.");
        }

        AtomicInteger sequence = new AtomicInteger(1);
        List<TemplateFieldMapping> mappings = requests.stream()
                .filter(request -> StringUtils.hasText(request.displayName()) || StringUtils.hasText(request.sourceLabel()))
                .map(request -> correctedMapping(tabId, request, sequence.getAndIncrement()))
                .toList();

        templateFieldMappingRepository.deleteAllByTabId(tabId);
        return templateFieldMappingRepository.saveAll(mappings).stream()
                .map(this::toMappingResponse)
                .toList();
    }

    @Transactional
    public List<TemplateFieldMappingResponse> improveRecommendations(Long tabId) {
        findTab(tabId);
        List<TemplateFieldMapping> mappings = templateFieldMappingRepository.findAllByTabIdOrderByCreatedAtAsc(tabId);
        for (TemplateFieldMapping mapping : mappings) {
            if (mapping.getMappingStatus() == MappingStatus.CONFIRMED
                    || mapping.getMappingStatus() == MappingStatus.EDITED
                    || mapping.getMappingStatus() == MappingStatus.CUSTOM) {
                continue;
            }

            String displayName = displayNameFromLabel(mapping.getSourceLabel());
            String description = descriptionFor(displayName, mapping.getSourceLabel());
            mapping.improveRecommendation(displayName, description, writingRuleFor(displayName), ConfidenceLevel.HIGH);
        }

        return templateFieldMappingRepository.saveAll(mappings).stream()
                .map(this::toMappingResponse)
                .toList();
    }

    private TemplateFieldMapping correctedMapping(Long tabId, TemplateFieldMappingUpdateRequest request, int sequence) {
        String fieldKey = StringUtils.hasText(request.fieldKey())
                ? request.fieldKey().trim()
                : StringUtils.hasText(request.semanticFieldKey()) && request.semanticFieldKey().startsWith("fld_")
                ? request.semanticFieldKey().trim()
                : fieldKey(sequence);
        String sourceLabel = normalizeText(request.sourceLabel());
        String displayName = StringUtils.hasText(request.displayName()) ? request.displayName().trim() : displayNameFromLabel(sourceLabel);
        String description = StringUtils.hasText(request.description()) ? request.description().trim() : descriptionFor(displayName, sourceLabel);
        MappingStatus status = mappingStatus(request.mappingStatus(), MappingStatus.EDITED);
        ConfidenceLevel confidenceLevel = confidenceLevel(request.confidenceLevel(), ConfidenceLevel.MEDIUM);

        return new TemplateFieldMapping(
                tabId,
                sourceLabel,
                fieldKey,
                fieldKey,
                displayName,
                description,
                Boolean.TRUE.equals(request.required()),
                status,
                confidenceLevel,
                normalizeNullable(request.writingRule()),
                confidenceValue(confidenceLevel)
        );
    }

    private List<TemplateFieldMappingResponse> saveAutoMappings(Long tabId, List<TemplateLabelResponse> labels) {
        templateFieldMappingRepository.deleteAllByTabId(tabId);

        Map<String, TemplateLabelResponse> uniqueLabels = new LinkedHashMap<>();
        for (TemplateLabelResponse label : labels) {
            uniqueLabels.putIfAbsent(normalizeText(label.text()), label);
        }

        AtomicInteger sequence = new AtomicInteger(1);
        List<TemplateFieldMapping> mappings = uniqueLabels.values().stream()
                .map(label -> suggestedMapping(tabId, label, sequence.getAndIncrement()))
                .toList();
        return templateFieldMappingRepository.saveAll(mappings).stream()
                .map(this::toMappingResponse)
                .toList();
    }

    private TemplateFieldMapping suggestedMapping(Long tabId, TemplateLabelResponse label, int sequence) {
        String fieldKey = fieldKey(sequence);
        String sourceLabel = normalizeText(label.text());
        String displayName = displayNameFromLabel(sourceLabel);
        ConfidenceLevel confidenceLevel = confidenceFor(label);
        return new TemplateFieldMapping(
                tabId,
                sourceLabel,
                fieldKey,
                fieldKey,
                displayName,
                descriptionFor(displayName, sourceLabel),
                false,
                MappingStatus.AUTO,
                confidenceLevel,
                writingRuleFor(displayName),
                confidenceValue(confidenceLevel)
        );
    }

    private ConfidenceLevel confidenceFor(TemplateLabelResponse label) {
        if (KNOWN_LABELS.contains(normalizeText(label.text())) || "TABLE_CELL".equals(label.type())) {
            return ConfidenceLevel.HIGH;
        }
        return normalizeText(label.text()).length() <= 16 ? ConfidenceLevel.MEDIUM : ConfidenceLevel.LOW;
    }

    private String fieldKey(int sequence) {
        return "fld_%03d".formatted(sequence);
    }

    private String displayNameFromLabel(String sourceLabel) {
        String normalized = normalizeText(sourceLabel);
        return StringUtils.hasText(normalized) ? stripTrailingPunctuation(normalized) : "사용자 필드";
    }

    private String descriptionFor(String displayName, String sourceLabel) {
        String label = StringUtils.hasText(sourceLabel) ? sourceLabel : displayName;
        return label + " 항목에 들어갈 내용을 사용자 입력 사실만 바탕으로 작성합니다.";
    }

    private String writingRuleFor(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            return null;
        }
        return displayName + "에 맞는 문서체로 정리하되, 입력되지 않은 사실은 생성하지 않습니다.";
    }

    private String stripTrailingPunctuation(String value) {
        return value.replaceAll("[:：\\-–—]+$", "").trim();
    }

    private MappingStatus mappingStatus(String value, MappingStatus fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return MappingStatus.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private ConfidenceLevel confidenceLevel(String value, ConfidenceLevel fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return ConfidenceLevel.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private double confidenceValue(ConfidenceLevel confidenceLevel) {
        return switch (confidenceLevel) {
            case HIGH -> 0.9;
            case MEDIUM -> 0.65;
            case LOW -> 0.35;
        };
    }

    private Tab findTab(Long tabId) {
        return tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("탭을 찾을 수 없습니다."));
    }

    private void validateTemplate(Tab tab) {
        if (!StringUtils.hasText(tab.getTemplatePath())) {
            throw new BadRequestException("분석할 DOCX 양식이 없습니다.");
        }

        if (!Files.exists(Path.of(tab.getTemplatePath()))) {
            throw new BadRequestException("업로드된 DOCX 양식 파일을 찾을 수 없습니다.");
        }
    }

    private TemplateAnalysis saveAnalysis(Long tabId, List<TemplateBlockResponse> blocks, List<TemplateLabelResponse> labels) {
        String rawStructureJson = writeRawStructure(new RawTemplateStructure(blocks, labels));
        TemplateAnalysis analysis = templateAnalysisRepository.findFirstByTabIdOrderByUpdatedAtDesc(tabId)
                .orElseGet(() -> new TemplateAnalysis(tabId, rawStructureJson));
        analysis.updateRawStructureJson(rawStructureJson);
        return templateAnalysisRepository.save(analysis);
    }

    private String writeRawStructure(RawTemplateStructure rawTemplateStructure) {
        try {
            return objectMapper.writeValueAsString(rawTemplateStructure);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("양식 분석 결과를 저장하지 못했습니다.");
        }
    }

    private RawTemplateStructure readRawStructure(String rawStructureJson) {
        try {
            return objectMapper.readValue(rawStructureJson, RawTemplateStructure.class);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("저장된 양식 분석 결과를 읽지 못했습니다.");
        }
    }

    private TemplateAnalysisResponse toResponse(
            Tab tab,
            TemplateAnalysis analysis,
            List<TemplateBlockResponse> blocks,
            List<TemplateLabelResponse> labels,
            List<TemplateFieldMappingResponse> mappings
    ) {
        return new TemplateAnalysisResponse(
                analysis.getId(),
                tab.getId(),
                tab.getName(),
                tab.getOriginalFileName(),
                blocks,
                labels,
                mappings,
                analysis.getUpdatedAt()
        );
    }

    private TemplateFieldMappingResponse toMappingResponse(TemplateFieldMapping mapping) {
        String fieldKey = mapping.getFieldKey();
        return new TemplateFieldMappingResponse(
                mapping.getId(),
                mapping.getSourceLabel(),
                fieldKey,
                fieldKey,
                mapping.getDisplayName(),
                mapping.getDescription(),
                mapping.getRequired(),
                mapping.getMappingStatus().name(),
                mapping.getConfidenceLevel().name(),
                mapping.getWritingRule(),
                mapping.getConfidence(),
                mapping.getCreatedAt(),
                mapping.getUpdatedAt()
        );
    }

    private List<TemplateBlockResponse> extractBlocks(XWPFDocument document) {
        List<TemplateBlockResponse> blocks = new ArrayList<>();
        int tableIndex = 0;

        for (IBodyElement element : document.getBodyElements()) {
            if (element.getElementType() == BodyElementType.PARAGRAPH) {
                addParagraphBlock(blocks, (XWPFParagraph) element);
                continue;
            }

            if (element.getElementType() == BodyElementType.TABLE) {
                addTableBlocks(blocks, (XWPFTable) element, tableIndex);
                tableIndex++;
            }
        }

        return blocks;
    }

    private void addParagraphBlock(List<TemplateBlockResponse> blocks, XWPFParagraph paragraph) {
        String text = normalizeText(paragraph.getText());
        if (!StringUtils.hasText(text)) {
            return;
        }

        blocks.add(new TemplateBlockResponse(
                "PARAGRAPH",
                blocks.size() + 1,
                text,
                null,
                null,
                null,
                isLabelCandidate(text, false)
        ));
    }

    private void addTableBlocks(List<TemplateBlockResponse> blocks, XWPFTable table, int tableIndex) {
        List<XWPFTableRow> rows = table.getRows();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<XWPFTableCell> cells = rows.get(rowIndex).getTableCells();
            for (int cellIndex = 0; cellIndex < cells.size(); cellIndex++) {
                String text = normalizeText(cells.get(cellIndex).getText());
                if (!StringUtils.hasText(text)) {
                    continue;
                }

                blocks.add(new TemplateBlockResponse(
                        "TABLE_CELL",
                        blocks.size() + 1,
                        text,
                        tableIndex,
                        rowIndex,
                        cellIndex,
                        isLabelCandidate(text, true)
                ));
            }
        }
    }

    private TemplateLabelResponse toLabel(TemplateBlockResponse block) {
        return new TemplateLabelResponse(
                block.text(),
                block.order(),
                block.type(),
                block.tableIndex(),
                block.rowIndex(),
                block.cellIndex()
        );
    }

    private boolean isLabelCandidate(String text, boolean tableCell) {
        String normalized = normalizeText(text);
        if (!StringUtils.hasText(normalized) || normalized.length() > MAX_LABEL_LENGTH) {
            return false;
        }

        String lowerCase = normalized.toLowerCase(Locale.ROOT);
        return KNOWN_LABELS.contains(normalized)
                || normalized.endsWith(":")
                || normalized.endsWith("：")
                || tableCell
                || looksLikeHeading(lowerCase);
    }

    private boolean looksLikeHeading(String text) {
        return text.length() <= MAX_LABEL_LENGTH
                && !text.endsWith(".")
                && !text.endsWith("!")
                && !text.endsWith("?")
                && !text.contains("\n");
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record RawTemplateStructure(
            List<TemplateBlockResponse> blocks,
            List<TemplateLabelResponse> labels
    ) {
    }
}
