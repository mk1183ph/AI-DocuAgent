package com.docuagent.localapp.controller;

import com.docuagent.localapp.ai.DocumentGenerationService;
import com.docuagent.localapp.dto.DocumentResponse;
import com.docuagent.localapp.dto.GenerateDraftRequest;
import com.docuagent.localapp.dto.TaskCreateRequest;
import com.docuagent.localapp.dto.TaskResponse;
import com.docuagent.localapp.dto.TaskUpdateRequest;
import com.docuagent.localapp.service.TaskService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;
    private final DocumentGenerationService documentGenerationService;

    public TaskController(TaskService taskService, DocumentGenerationService documentGenerationService) {
        this.taskService = taskService;
        this.documentGenerationService = documentGenerationService;
    }

    @GetMapping("/tabs/{tabId}/tasks")
    public List<TaskResponse> findAllByTabId(@PathVariable Long tabId) {
        return taskService.findAllByTabId(tabId);
    }

    @PostMapping("/tabs/{tabId}/tasks")
    public ResponseEntity<TaskResponse> create(
            @PathVariable Long tabId,
            @Valid @RequestBody TaskCreateRequest request
    ) {
        TaskResponse response = taskService.create(tabId, request);
        return ResponseEntity.created(URI.create("/api/tasks/" + response.id())).body(response);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse findById(@PathVariable Long taskId) {
        return taskService.findById(taskId);
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse update(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request
    ) {
        return taskService.update(taskId, request);
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long taskId) {
        taskService.delete(taskId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tasks/{taskId}/generate-draft")
    public DocumentResponse generateDraft(
            @PathVariable Long taskId,
            @Valid @RequestBody(required = false) GenerateDraftRequest request
    ) {
        return documentGenerationService.generateDraft(
                taskId,
                request == null ? null : request.inferenceStrength()
        );
    }
}
