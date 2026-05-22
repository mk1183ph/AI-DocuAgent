package com.docuagent.localapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tabs")
public class Tab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "template_path")
    private String templatePath;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "base_prompt", length = 4000)
    private String basePrompt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Tab() {
    }

    public Tab(
            String name,
            String description,
            String basePrompt,
            String originalFileName,
            String templatePath
    ) {
        this.name = name;
        this.description = description;
        this.basePrompt = basePrompt;
        this.originalFileName = originalFileName;
        this.templatePath = templatePath;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void update(String name, String description, String basePrompt) {
        this.name = name;
        this.description = description;
        this.basePrompt = basePrompt;
    }

    public void updateTemplate(String originalFileName, String templatePath) {
        this.originalFileName = originalFileName;
        this.templatePath = templatePath;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getBasePrompt() {
        return basePrompt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
