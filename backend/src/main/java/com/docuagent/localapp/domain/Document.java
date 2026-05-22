package com.docuagent.localapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "generated_content", length = 12000)
    private String generatedContent;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "reconstruction_summary", length = 20000)
    private String reconstructionSummary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Document() {
    }

    public Document(Long taskId, String generatedContent, String filePath) {
        this.taskId = taskId;
        this.generatedContent = generatedContent;
        this.filePath = filePath;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateFilePath(String filePath) {
        this.filePath = filePath;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateReconstructionSummary(String reconstructionSummary) {
        this.reconstructionSummary = reconstructionSummary;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getGeneratedContent() {
        return generatedContent;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getReconstructionSummary() {
        return reconstructionSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
