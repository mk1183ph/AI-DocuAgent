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
@Table(name = "template_field_mappings")
public class TemplateFieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tab_id", nullable = false)
    private Long tabId;

    @Column(name = "source_label", nullable = false, length = 200)
    private String sourceLabel;

    @Column(name = "semantic_field_key", length = 100)
    private String semanticFieldKey;

    @Column(name = "field_key", length = 100)
    private String fieldKey;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 1000)
    private String description;

    @Column(name = "is_required")
    private Boolean required;

    @Column(name = "mapping_status", length = 30)
    private String mappingStatus;

    @Column(name = "confidence_level", length = 30)
    private String confidenceLevel;

    @Column(name = "writing_rule", length = 1000)
    private String writingRule;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected TemplateFieldMapping() {
    }

    public TemplateFieldMapping(
            Long tabId,
            String sourceLabel,
            String semanticFieldKey,
            String fieldKey,
            String displayName,
            String description,
            Boolean required,
            MappingStatus mappingStatus,
            ConfidenceLevel confidenceLevel,
            String writingRule,
            Double confidence
    ) {
        this.tabId = tabId;
        this.sourceLabel = sourceLabel;
        this.semanticFieldKey = semanticFieldKey;
        this.fieldKey = fieldKey;
        this.displayName = displayName;
        this.description = description;
        this.required = required;
        this.mappingStatus = mappingStatus == null ? MappingStatus.AUTO.name() : mappingStatus.name();
        this.confidenceLevel = confidenceLevel == null ? ConfidenceLevel.LOW.name() : confidenceLevel.name();
        this.writingRule = writingRule;
        this.confidence = confidence;
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

    public void assignFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
        if (semanticFieldKey == null || semanticFieldKey.isBlank()) {
            semanticFieldKey = fieldKey;
        }
    }

    public void updateDynamicFields(
            String sourceLabel,
            String displayName,
            String description,
            Boolean required,
            MappingStatus mappingStatus,
            String writingRule
    ) {
        this.sourceLabel = sourceLabel;
        this.displayName = displayName;
        this.description = description;
        this.required = required;
        this.mappingStatus = mappingStatus == null ? MappingStatus.EDITED.name() : mappingStatus.name();
        this.writingRule = writingRule;
        this.updatedAt = LocalDateTime.now();
    }

    public void improveRecommendation(String displayName, String description, String writingRule, ConfidenceLevel confidenceLevel) {
        this.displayName = displayName;
        this.description = description;
        this.writingRule = writingRule;
        this.confidenceLevel = confidenceLevel == null ? this.confidenceLevel : confidenceLevel.name();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTabId() {
        return tabId;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getSemanticFieldKey() {
        return semanticFieldKey;
    }

    public String getFieldKey() {
        return fieldKey == null || fieldKey.isBlank() ? semanticFieldKey : fieldKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getRequired() {
        return required;
    }

    public MappingStatus getMappingStatus() {
        if (mappingStatus == null || mappingStatus.isBlank()) {
            return MappingStatus.AUTO;
        }
        try {
            return MappingStatus.valueOf(mappingStatus);
        } catch (IllegalArgumentException exception) {
            return MappingStatus.AUTO;
        }
    }

    public ConfidenceLevel getConfidenceLevel() {
        if (confidenceLevel == null || confidenceLevel.isBlank()) {
            return ConfidenceLevel.LOW;
        }
        try {
            return ConfidenceLevel.valueOf(confidenceLevel);
        } catch (IllegalArgumentException exception) {
            return ConfidenceLevel.LOW;
        }
    }

    public String getWritingRule() {
        return writingRule;
    }

    public Double getConfidence() {
        return confidence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
