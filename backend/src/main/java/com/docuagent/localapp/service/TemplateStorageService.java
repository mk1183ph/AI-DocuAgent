package com.docuagent.localapp.service;

import com.docuagent.localapp.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TemplateStorageService {

    private final Path templatesDirectory;

    public TemplateStorageService(@Value("${app.storage.templates-dir}") String templatesDirectory) {
        this.templatesDirectory = Path.of(templatesDirectory).normalize();
    }

    public StoredTemplate storeRequired(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("DOCX 템플릿 파일을 업로드하세요");
        }
        return store(file);
    }

    public Optional<StoredTemplate> storeOptional(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(store(file));
    }

    private StoredTemplate store(MultipartFile file) {
        String originalFileName = sanitizeOriginalFileName(file.getOriginalFilename());
        validateDocx(file, originalFileName);

        try {
            Files.createDirectories(templatesDirectory);
            String storedFileName = UUID.randomUUID() + ".docx";
            Path target = templatesDirectory.resolve(storedFileName).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredTemplate(originalFileName, target.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("템플릿 파일을 저장하지 못했습니다", exception);
        }
    }

    private void validateDocx(MultipartFile file, String originalFileName) {
        if (!originalFileName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new BadRequestException("DOCX 파일만 업로드할 수 있습니다");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return;
        }

        if (!contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                && !contentType.equals("application/octet-stream")) {
            throw new BadRequestException("DOCX 파일만 업로드할 수 있습니다");
        }
    }

    private String sanitizeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "template.docx";
        }
        return Path.of(originalFileName).getFileName().toString();
    }
}
