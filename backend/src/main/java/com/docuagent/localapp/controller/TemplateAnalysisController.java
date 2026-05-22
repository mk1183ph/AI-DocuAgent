package com.docuagent.localapp.controller;

import com.docuagent.localapp.dto.TemplateAnalysisResponse;
import com.docuagent.localapp.dto.TemplateFieldMappingResponse;
import com.docuagent.localapp.dto.TemplateFieldMappingUpdateRequest;
import com.docuagent.localapp.template.analysis.TemplateAnalysisService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tabs")
public class TemplateAnalysisController {

    private final TemplateAnalysisService templateAnalysisService;

    public TemplateAnalysisController(TemplateAnalysisService templateAnalysisService) {
        this.templateAnalysisService = templateAnalysisService;
    }

    @PostMapping("/{tabId}/analyze-template")
    public TemplateAnalysisResponse analyze(@PathVariable Long tabId) {
        return templateAnalysisService.analyze(tabId);
    }

    @GetMapping("/{tabId}/template-analysis")
    public TemplateAnalysisResponse findLatestAnalysis(@PathVariable Long tabId) {
        return templateAnalysisService.findLatestAnalysis(tabId);
    }

    @GetMapping("/{tabId}/template-mappings")
    public List<TemplateFieldMappingResponse> findMappings(@PathVariable Long tabId) {
        return templateAnalysisService.findMappings(tabId);
    }

    @GetMapping("/{tabId}/template-fields")
    public List<TemplateFieldMappingResponse> findFields(@PathVariable Long tabId) {
        return templateAnalysisService.findMappings(tabId);
    }

    @PutMapping("/{tabId}/template-mappings")
    public List<TemplateFieldMappingResponse> updateMappings(
            @PathVariable Long tabId,
            @Valid @RequestBody List<@Valid TemplateFieldMappingUpdateRequest> request
    ) {
        return templateAnalysisService.updateMappings(tabId, request);
    }

    @PutMapping("/{tabId}/template-fields")
    public List<TemplateFieldMappingResponse> updateFields(
            @PathVariable Long tabId,
            @Valid @RequestBody List<@Valid TemplateFieldMappingUpdateRequest> request
    ) {
        return templateAnalysisService.updateMappings(tabId, request);
    }

    @PostMapping("/{tabId}/template-fields/recommendations")
    public List<TemplateFieldMappingResponse> improveRecommendations(@PathVariable Long tabId) {
        return templateAnalysisService.improveRecommendations(tabId);
    }
}
