package com.docuagent.localapp.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TemplateAnalysisResponse(
        Long analysisId,
        Long tabId,
        String tabName,
        String originalFileName,
        List<TemplateBlockResponse> blocks,
        List<TemplateLabelResponse> labels,
        List<TemplateFieldMappingResponse> mappings,
        LocalDateTime updatedAt
) {
}
