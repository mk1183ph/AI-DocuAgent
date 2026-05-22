package com.docuagent.localapp.service;

import com.docuagent.localapp.domain.Tab;
import com.docuagent.localapp.dto.TabCreateRequest;
import com.docuagent.localapp.dto.TabResponse;
import com.docuagent.localapp.dto.TabUpdateRequest;
import com.docuagent.localapp.exception.ResourceNotFoundException;
import com.docuagent.localapp.repository.DocumentRepository;
import com.docuagent.localapp.repository.TabRepository;
import com.docuagent.localapp.repository.TaskRepository;
import com.docuagent.localapp.repository.TemplateAnalysisRepository;
import com.docuagent.localapp.repository.TemplateFieldMappingRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TabService {

    private final TabRepository tabRepository;
    private final TemplateStorageService templateStorageService;
    private final TaskRepository taskRepository;
    private final DocumentRepository documentRepository;
    private final TemplateAnalysisRepository templateAnalysisRepository;
    private final TemplateFieldMappingRepository templateFieldMappingRepository;

    public TabService(
            TabRepository tabRepository,
            TemplateStorageService templateStorageService,
            TaskRepository taskRepository,
            DocumentRepository documentRepository,
            TemplateAnalysisRepository templateAnalysisRepository,
            TemplateFieldMappingRepository templateFieldMappingRepository
    ) {
        this.tabRepository = tabRepository;
        this.templateStorageService = templateStorageService;
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.templateAnalysisRepository = templateAnalysisRepository;
        this.templateFieldMappingRepository = templateFieldMappingRepository;
    }

    @Transactional(readOnly = true)
    public List<TabResponse> findAll() {
        return tabRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TabResponse create(TabCreateRequest request) {
        StoredTemplate storedTemplate = templateStorageService.storeRequired(request.templateFile());
        Tab tab = new Tab(
                request.name().trim(),
                normalizeBlank(request.description()),
                normalizeBlank(request.basePrompt()),
                storedTemplate.originalFileName(),
                storedTemplate.templatePath()
        );
        return toResponse(tabRepository.save(tab));
    }

    @Transactional(readOnly = true)
    public TabResponse findById(Long id) {
        return toResponse(getTab(id));
    }

    @Transactional
    public TabResponse update(Long id, TabUpdateRequest request) {
        Tab tab = getTab(id);
        tab.update(
                request.name().trim(),
                normalizeBlank(request.description()),
                normalizeBlank(request.basePrompt())
        );
        templateStorageService.storeOptional(request.templateFile())
                .ifPresent(storedTemplate -> tab.updateTemplate(
                        storedTemplate.originalFileName(),
                        storedTemplate.templatePath()
                ));
        return toResponse(tab);
    }

    @Transactional
    public void delete(Long id) {
        Tab tab = getTab(id);
        List<Long> taskIds = taskRepository.findAllByTabIdOrderByCreatedAtDesc(id)
                .stream()
                .map(com.docuagent.localapp.domain.Task::getId)
                .toList();
        if (!taskIds.isEmpty()) {
            documentRepository.deleteAllByTaskIdIn(taskIds);
        }
        taskRepository.deleteAllByTabId(id);
        templateAnalysisRepository.deleteAllByTabId(id);
        templateFieldMappingRepository.deleteAllByTabId(id);
        tabRepository.delete(tab);
    }

    private Tab getTab(Long id) {
        return tabRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tab not found: " + id));
    }

    private TabResponse toResponse(Tab tab) {
        return new TabResponse(
                tab.getId(),
                tab.getName(),
                tab.getDescription(),
                tab.getOriginalFileName(),
                tab.getTemplatePath(),
                tab.getBasePrompt(),
                tab.getCreatedAt()
        );
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
