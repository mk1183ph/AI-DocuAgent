package com.docuagent.localapp.domain;

import com.docuagent.localapp.ai.generator.AiProviderType;
import com.docuagent.localapp.ai.prompt.AiWritingMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "settings")
public class Settings {

    public static final Long SINGLETON_ID = 1L;
    public static final AiProviderType DEFAULT_AI_PROVIDER = AiProviderType.OLLAMA;
    public static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
    public static final String DEFAULT_OLLAMA_MODEL = "qwen2.5:7b";
    public static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";
    public static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 300;
    public static final AiWritingMode DEFAULT_AI_WRITING_MODE = AiWritingMode.BALANCED;

    @Id
    private Long id;

    @Column(name = "ai_provider", nullable = false, length = 30)
    private String aiProvider;

    @Column(name = "ai_writing_mode", nullable = false, length = 30)
    private String aiWritingMode;

    @Column(name = "ollama_base_url", nullable = false, length = 300)
    private String ollamaBaseUrl;

    @Column(name = "ollama_model", nullable = false, length = 120)
    private String ollamaModel;

    @Column(name = "gemini_api_key", length = 500)
    private String geminiApiKey;

    @Column(name = "gemini_model", length = 120)
    private String geminiModel;

    @Column(name = "request_timeout_seconds", nullable = false)
    private Integer requestTimeoutSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Settings() {
    }

    public Settings(Long id) {
        this.id = id;
        this.aiProvider = DEFAULT_AI_PROVIDER.name();
        this.aiWritingMode = DEFAULT_AI_WRITING_MODE.name();
        this.ollamaBaseUrl = DEFAULT_OLLAMA_BASE_URL;
        this.ollamaModel = DEFAULT_OLLAMA_MODEL;
        this.geminiModel = DEFAULT_GEMINI_MODEL;
        this.requestTimeoutSeconds = DEFAULT_REQUEST_TIMEOUT_SECONDS;
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

    public void updateAiSettings(
            AiProviderType aiProvider,
            String ollamaBaseUrl,
            String ollamaModel,
            String geminiApiKey,
            String geminiModel,
            Integer requestTimeoutSeconds,
            AiWritingMode aiWritingMode
    ) {
        this.aiProvider = aiProvider == null ? DEFAULT_AI_PROVIDER.name() : aiProvider.name();
        this.aiWritingMode = aiWritingMode == null ? DEFAULT_AI_WRITING_MODE.name() : aiWritingMode.name();
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ollamaModel = ollamaModel;
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public AiProviderType getAiProvider() {
        return AiProviderType.valueOf(aiProvider);
    }

    public AiWritingMode getAiWritingMode() {
        return AiWritingMode.valueOf(aiWritingMode);
    }

    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public Integer getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
