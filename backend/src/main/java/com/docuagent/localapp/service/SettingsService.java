package com.docuagent.localapp.service;

import com.docuagent.localapp.ai.generator.AiGenerationOptions;
import com.docuagent.localapp.ai.generator.AiProviderType;
import com.docuagent.localapp.domain.Settings;
import com.docuagent.localapp.dto.SettingsResponse;
import com.docuagent.localapp.dto.SettingsUpdateRequest;
import com.docuagent.localapp.exception.BadRequestException;
import com.docuagent.localapp.repository.SettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;

    public SettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Transactional
    public Settings getSettingsEntity() {
        return settingsRepository.findById(Settings.SINGLETON_ID)
                .orElseGet(() -> settingsRepository.save(new Settings(Settings.SINGLETON_ID)));
    }

    @Transactional
    public SettingsResponse find() {
        Settings settings = settingsRepository.findById(Settings.SINGLETON_ID)
                .orElseGet(() -> settingsRepository.save(new Settings(Settings.SINGLETON_ID)));
        return toResponse(settings);
    }

    @Transactional
    public SettingsResponse update(SettingsUpdateRequest request) {
        Settings settings = settingsRepository.findById(Settings.SINGLETON_ID)
                .orElseGet(() -> settingsRepository.save(new Settings(Settings.SINGLETON_ID)));
        validateProviderSettings(request);
        settings.updateAiSettings(
                request.aiProvider(),
                withDefault(request.ollamaBaseUrl(), Settings.DEFAULT_OLLAMA_BASE_URL),
                withDefault(request.ollamaModel(), Settings.DEFAULT_OLLAMA_MODEL),
                normalizeSecret(request.geminiApiKey()),
                withDefault(request.geminiModel(), Settings.DEFAULT_GEMINI_MODEL),
                request.requestTimeoutSeconds(),
                request.aiWritingMode()
        );
        return toResponse(settings);
    }

    public AiGenerationOptions toGenerationOptions(Settings settings) {
        return new AiGenerationOptions(
                withDefault(settings.getOllamaBaseUrl(), Settings.DEFAULT_OLLAMA_BASE_URL),
                withDefault(settings.getOllamaModel(), Settings.DEFAULT_OLLAMA_MODEL),
                normalizeSecret(settings.getGeminiApiKey()),
                withDefault(settings.getGeminiModel(), Settings.DEFAULT_GEMINI_MODEL),
                settings.getRequestTimeoutSeconds()
        );
    }

    private SettingsResponse toResponse(Settings settings) {
        return new SettingsResponse(
                settings.getAiProvider(),
                settings.getAiWritingMode(),
                withDefault(settings.getOllamaBaseUrl(), Settings.DEFAULT_OLLAMA_BASE_URL),
                withDefault(settings.getOllamaModel(), Settings.DEFAULT_OLLAMA_MODEL),
                settings.getGeminiApiKey(),
                withDefault(settings.getGeminiModel(), Settings.DEFAULT_GEMINI_MODEL),
                settings.getRequestTimeoutSeconds(),
                settings.getUpdatedAt()
        );
    }

    private void validateProviderSettings(SettingsUpdateRequest request) {
        if (request.aiProvider() == AiProviderType.OLLAMA) {
            if (!StringUtils.hasText(request.ollamaBaseUrl())) {
                throw new BadRequestException("Ollama URL을 입력해주세요.");
            }
            if (!StringUtils.hasText(request.ollamaModel())) {
                throw new BadRequestException("Ollama 모델 이름을 입력해주세요.");
            }
        }

        if (request.aiProvider() == AiProviderType.GEMINI) {
            if (!StringUtils.hasText(request.geminiApiKey())) {
                throw new BadRequestException("Gemini API 키를 입력해주세요.");
            }
            if (!StringUtils.hasText(request.geminiModel())) {
                throw new BadRequestException("Gemini 모델 이름을 입력해주세요.");
            }
        }
    }

    private String withDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String normalizeSecret(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
