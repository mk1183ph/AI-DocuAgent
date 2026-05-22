package com.docuagent.localapp.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LocalDirectoryInitializer implements ApplicationRunner {

    private static final String SQLITE_PREFIX = "jdbc:sqlite:";

    private final String templatesDirectory;
    private final String generatedDirectory;
    private final String datasourceUrl;

    public LocalDirectoryInitializer(
            @Value("${app.storage.templates-dir}") String templatesDirectory,
            @Value("${app.storage.generated-dir}") String generatedDirectory,
            @Value("${spring.datasource.url}") String datasourceUrl
    ) {
        this.templatesDirectory = templatesDirectory;
        this.generatedDirectory = generatedDirectory;
        this.datasourceUrl = datasourceUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        createDirectory(Path.of(templatesDirectory));
        createDirectory(Path.of(generatedDirectory));
        sqlitePath().ifPresent(path -> {
            Path parent = path.getParent();
            if (parent != null) {
                createDirectory(parent);
            }
        });
    }

    private Optional<Path> sqlitePath() {
        if (!StringUtils.hasText(datasourceUrl) || !datasourceUrl.startsWith(SQLITE_PREFIX)) {
            return Optional.empty();
        }

        String rawPath = datasourceUrl.substring(SQLITE_PREFIX.length()).trim();
        if (!StringUtils.hasText(rawPath) || rawPath.equals(":memory:")) {
            return Optional.empty();
        }

        if (rawPath.startsWith("file:")) {
            rawPath = rawPath.substring("file:".length());
        }

        return Optional.of(Path.of(rawPath).normalize());
    }

    private void createDirectory(Path directory) {
        try {
            Files.createDirectories(directory.normalize());
        } catch (IOException exception) {
            throw new IllegalStateException("로컬 저장소 디렉터리를 준비하지 못했습니다: " + directory, exception);
        }
    }
}
