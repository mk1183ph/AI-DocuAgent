package com.docuagent.localapp.config;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SettingsSchemaMigration implements ApplicationRunner {

    private static final String TABLE_NAME = "settings";
    private static final String TEMP_TABLE_NAME = "settings_migrated";

    private final JdbcTemplate jdbcTemplate;

    public SettingsSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String createSql = findSettingsCreateSql();
        if (createSql == null) {
            return;
        }

        Set<String> columns = settingsColumns();
        if (!hasProviderCheckConstraint(createSql)
                && columns.contains("ai_writing_mode")
                && !hasNullWritingMode()) {
            return;
        }

        jdbcTemplate.execute("drop table if exists " + TEMP_TABLE_NAME);
        jdbcTemplate.execute("""
                create table settings_migrated (
                    id bigint not null,
                    ai_provider varchar(30) not null,
                    ai_writing_mode varchar(30) not null,
                    created_at timestamp not null,
                    ollama_base_url varchar(300) not null,
                    ollama_model varchar(120) not null,
                    request_timeout_seconds integer not null,
                    updated_at timestamp not null,
                    gemini_api_key varchar(500),
                    gemini_model varchar(120),
                    primary key (id)
                )
                """);
        jdbcTemplate.execute(insertMigrationSql(columns));
        jdbcTemplate.execute("drop table " + TABLE_NAME);
        jdbcTemplate.execute("alter table " + TEMP_TABLE_NAME + " rename to " + TABLE_NAME);
    }

    private String findSettingsCreateSql() {
        List<String> results = jdbcTemplate.query(
                "select sql from sqlite_master where type = 'table' and name = ?",
                (resultSet, rowNumber) -> resultSet.getString("sql"),
                TABLE_NAME
        );
        return results.isEmpty() ? null : results.get(0);
    }

    private boolean hasProviderCheckConstraint(String createSql) {
        String normalized = createSql.toLowerCase(Locale.ROOT);
        return normalized.contains("ai_provider")
                && normalized.contains("check")
                && normalized.contains("ollama")
                && normalized.contains("mock");
    }

    private Set<String> settingsColumns() {
        return jdbcTemplate.queryForList("pragma table_info(" + TABLE_NAME + ")").stream()
                .map(row -> String.valueOf(row.get("name")))
                .collect(Collectors.toSet());
    }

    private boolean hasNullWritingMode() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from settings where ai_writing_mode is null",
                    Integer.class
            );
            return count != null && count > 0;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String insertMigrationSql(Set<String> columns) {
        return """
                insert into settings_migrated (
                    id,
                    ai_provider,
                    ai_writing_mode,
                    created_at,
                    ollama_base_url,
                    ollama_model,
                    request_timeout_seconds,
                    updated_at,
                    gemini_api_key,
                    gemini_model
                )
                select
                    %s,
                    %s,
                    %s,
                    %s,
                    %s,
                    %s,
                    %s,
                    %s,
                    %s,
                    %s
                from settings
                """.formatted(
                expression(columns, "id", "1"),
                expression(columns, "ai_provider", "'OLLAMA'"),
                expression(columns, "ai_writing_mode", "'BALANCED'"),
                expression(columns, "created_at", "current_timestamp"),
                expression(columns, "ollama_base_url", "'http://localhost:11434'"),
                expression(columns, "ollama_model", "'qwen2.5:7b'"),
                expression(columns, "request_timeout_seconds", "300"),
                expression(columns, "updated_at", "current_timestamp"),
                expression(columns, "gemini_api_key", "null"),
                expression(columns, "gemini_model", "'gemini-2.5-flash'")
        );
    }

    private String expression(Set<String> columns, String columnName, String defaultValue) {
        if (!columns.contains(columnName)) {
            return defaultValue;
        }
        if (Map.of(
                "ollama_base_url", "'http://localhost:11434'",
                "ollama_model", "'qwen2.5:7b'",
                "gemini_model", "'gemini-2.5-flash'",
                "ai_writing_mode", "'BALANCED'"
        ).containsKey(columnName)) {
            return "coalesce(" + columnName + ", " + defaultValue + ")";
        }
        return columnName;
    }
}
