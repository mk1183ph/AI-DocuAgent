package com.docuagent.localapp.controller;

import com.docuagent.localapp.dto.DocumentResponse;
import com.docuagent.localapp.dto.DocumentUpdateRequest;
import com.docuagent.localapp.service.DocumentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/tasks/{taskId}/documents")
    public List<DocumentResponse> findAllByTaskId(@PathVariable Long taskId) {
        return documentService.findAllByTaskId(taskId);
    }

    @GetMapping("/tasks/{taskId}/documents/latest")
    public DocumentResponse findLatestByTaskId(@PathVariable Long taskId) {
        return documentService.findLatestByTaskId(taskId);
    }

    @GetMapping("/documents/{documentId}")
    public DocumentResponse findById(@PathVariable Long documentId) {
        return documentService.findById(documentId);
    }

    @PutMapping("/documents/{documentId}")
    public DocumentResponse update(
            @PathVariable Long documentId,
            @Valid @RequestBody DocumentUpdateRequest request
    ) {
        return documentService.update(documentId, request);
    }
}
