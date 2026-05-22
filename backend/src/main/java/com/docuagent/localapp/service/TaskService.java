package com.docuagent.localapp.service;

import com.docuagent.localapp.domain.Task;
import com.docuagent.localapp.dto.TaskCreateRequest;
import com.docuagent.localapp.dto.TaskResponse;
import com.docuagent.localapp.dto.TaskUpdateRequest;
import com.docuagent.localapp.exception.ResourceNotFoundException;
import com.docuagent.localapp.repository.TabRepository;
import com.docuagent.localapp.repository.TaskRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private static final String DEFAULT_STATUS = "DRAFT";

    private final TaskRepository taskRepository;
    private final TabRepository tabRepository;

    public TaskService(TaskRepository taskRepository, TabRepository tabRepository) {
        this.taskRepository = taskRepository;
        this.tabRepository = tabRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAllByTabId(Long tabId) {
        ensureTabExists(tabId);
        return taskRepository.findAllByTabIdOrderByCreatedAtDesc(tabId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskResponse create(Long tabId, TaskCreateRequest request) {
        ensureTabExists(tabId);
        Task task = new Task(
                tabId,
                request.title().trim(),
                request.userContext().trim(),
                DEFAULT_STATUS
        );
        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long taskId) {
        return toResponse(getTask(taskId));
    }

    @Transactional
    public TaskResponse update(Long taskId, TaskUpdateRequest request) {
        Task task = getTask(taskId);
        task.update(request.title().trim(), request.userContext().trim());
        return toResponse(task);
    }

    @Transactional
    public void delete(Long taskId) {
        Task task = getTask(taskId);
        taskRepository.delete(task);
    }

    private void ensureTabExists(Long tabId) {
        if (!tabRepository.existsById(tabId)) {
            throw new ResourceNotFoundException("Tab not found: " + tabId);
        }
    }

    private Task getTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTabId(),
                task.getTitle(),
                task.getUserContext(),
                task.getStatus(),
                task.getCreatedAt()
        );
    }
}
