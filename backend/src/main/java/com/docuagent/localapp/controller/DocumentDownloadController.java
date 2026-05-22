package com.docuagent.localapp.controller;

import com.docuagent.localapp.dto.ReconstructedDocumentResponse;
import com.docuagent.localapp.dto.ReconstructionSummaryResponse;
import com.docuagent.localapp.service.DocumentReconstructionService;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class DocumentDownloadController {

    private static final MediaType DOCX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final DocumentReconstructionService documentReconstructionService;

    public DocumentDownloadController(DocumentReconstructionService documentReconstructionService) {
        this.documentReconstructionService = documentReconstructionService;
    }

    @GetMapping("/{taskId}/download-docx")
    public ResponseEntity<Resource> downloadDocx(@PathVariable Long taskId) {
        ReconstructedDocumentResponse reconstructedDocument = documentReconstructionService.reconstruct(taskId);
        Resource resource = new FileSystemResource(reconstructedDocument.filePath());

        return ResponseEntity.ok()
                .contentType(DOCX_MEDIA_TYPE)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(reconstructedDocument.downloadFileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(resource);
    }

    @GetMapping("/{taskId}/reconstruction-summary")
    public ReconstructionSummaryResponse reconstructionSummary(@PathVariable Long taskId) {
        return documentReconstructionService.findLatestSummary(taskId);
    }
}
