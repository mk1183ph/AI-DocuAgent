package com.docuagent.localapp.service;

import com.docuagent.localapp.domain.Document;
import com.docuagent.localapp.dto.DocumentResponse;
import com.docuagent.localapp.dto.DocumentUpdateRequest;
import com.docuagent.localapp.exception.ResourceNotFoundException;
import com.docuagent.localapp.repository.DocumentRepository;
import com.docuagent.localapp.repository.TaskRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final TaskRepository taskRepository;
    private final DocumentResponseMapper documentResponseMapper;

    public DocumentService(
            DocumentRepository documentRepository,
            TaskRepository taskRepository,
            DocumentResponseMapper documentResponseMapper
    ) {
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.documentResponseMapper = documentResponseMapper;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> findAllByTaskId(Long taskId) {
        ensureTaskExists(taskId);
        return documentRepository.findAllByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse findLatestByTaskId(Long taskId) {
        ensureTaskExists(taskId);
        return documentRepository.findFirstByTaskIdOrderByCreatedAtDesc(taskId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found for task: " + taskId));
    }

    @Transactional(readOnly = true)
    public DocumentResponse findById(Long documentId) {
        return toResponse(getDocument(documentId));
    }

    @Transactional
    public DocumentResponse update(Long documentId, DocumentUpdateRequest request) {
        Document document = getDocument(documentId);
        document.updateGeneratedContent(request.generatedContent().trim());
        return toResponse(document);
    }

    private void ensureTaskExists(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task not found: " + taskId);
        }
    }

    private Document getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
    }

    private DocumentResponse toResponse(Document document) {
        return documentResponseMapper.toResponse(document);
    }
}
