package com.docuagent.localapp.config;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TemplateFieldMappingSchemaMigration implements ApplicationRunner {

    private static final String TABLE_NAME = "template_field_mappings";

    private final JdbcTemplate jdbcTemplate;

    public TemplateFieldMappingSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists()) {
            return;
        }

        Set<String> columns = columns();
        addColumnIfMissing(columns, "field_key", "varchar(100)");
        addColumnIfMissing(columns, "description", "varchar(1000)");
        addColumnIfMissing(columns, "is_required", "boolean");
        addColumnIfMissing(columns, "mapping_status", "varchar(30)");
        addColumnIfMissing(columns, "confidence_level", "varchar(30)");
        addColumnIfMissing(columns, "writing_rule", "varchar(1000)");
        addColumnIfMissing(columns, "updated_at", "timestamp");
        if (!hasRowsNeedingBackfill()) {
            return;
        }
        backfillDynamicSchema();
    }

    private boolean tableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sqlite_master where type = 'table' and name = ?",
                Integer.class,
                TABLE_NAME
        );
        return count != null && count > 0;
    }

    private Set<String> columns() {
        Set<String> names = new HashSet<>();
        jdbcTemplate.queryForList("pragma table_info(" + TABLE_NAME + ")")
                .forEach(row -> names.add(String.valueOf(row.get("name"))));
        return names;
    }

    private void addColumnIfMissing(Set<String> columns, String columnName, String definition) {
        if (columns.contains(columnName)) {
            return;
        }
        jdbcTemplate.execute("alter table " + TABLE_NAME + " add column " + columnName + " " + definition);
        columns.add(columnName);
    }

    private boolean hasRowsNeedingBackfill() {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from template_field_mappings
                where field_key is null or trim(field_key) = ''
                   or description is null or trim(description) = ''
                   or mapping_status is null or trim(mapping_status) = ''
                   or confidence_level is null or trim(confidence_level) = ''
                   or updated_at is null
                """,
                Integer.class
        );
        return count != null && count > 0;
    }

    private void backfillDynamicSchema() {
        List<MappingRow> rows = jdbcTemplate.query(
                "select id, tab_id, source_label, semantic_field_key, field_key, display_name, confidence, created_at from "
                        + TABLE_NAME + " order by tab_id, id",
                (resultSet, rowNumber) -> toRow(resultSet)
        );

        Long currentTabId = null;
        int sequence = 1;
        for (MappingRow row : rows) {
            if (!row.tabId().equals(currentTabId)) {
                currentTabId = row.tabId();
                sequence = 1;
            }

            String fieldKey = StringUtils.hasText(row.fieldKey()) ? row.fieldKey() : "fld_%03d".formatted(sequence);
            String displayName = StringUtils.hasText(row.displayName()) ? row.displayName() : fallbackDisplayName(row.sourceLabel());
            String confidenceLevel = confidenceLevel(row.confidence());
            jdbcTemplate.update(
                    """
                    update template_field_mappings
                    set field_key = ?,
                        semantic_field_key = coalesce(nullif(semantic_field_key, ''), ?),
                        display_name = ?,
                        description = coalesce(nullif(description, ''), ?),
                        is_required = coalesce(is_required, 0),
                        mapping_status = coalesce(nullif(mapping_status, ''), 'CONFIRMED'),
                        confidence_level = coalesce(nullif(confidence_level, ''), ?),
                        updated_at = coalesce(updated_at, ?)
                    where id = ?
                    """,
                    fieldKey,
                    fieldKey,
                    displayName,
                    descriptionFor(displayName, row.sourceLabel()),
                    confidenceLevel,
                    row.createdAt() == null ? Timestamp.valueOf(LocalDateTime.now()) : row.createdAt(),
                    row.id()
            );
            sequence++;
        }
    }

    private MappingRow toRow(ResultSet resultSet) throws SQLException {
        return new MappingRow(
                resultSet.getLong("id"),
                resultSet.getLong("tab_id"),
                resultSet.getString("source_label"),
                resultSet.getString("semantic_field_key"),
                resultSet.getString("field_key"),
                resultSet.getString("display_name"),
                resultSet.getObject("confidence") == null ? null : resultSet.getDouble("confidence"),
                resultSet.getTimestamp("created_at")
        );
    }

    private String fallbackDisplayName(String sourceLabel) {
        return StringUtils.hasText(sourceLabel)
                ? sourceLabel.replaceAll("[:：\\-–—]+$", "").trim()
                : "사용자 필드";
    }

    private String descriptionFor(String displayName, String sourceLabel) {
        String label = StringUtils.hasText(sourceLabel) ? sourceLabel : displayName;
        return label + " 항목에 들어갈 내용을 사용자 입력 사실만 바탕으로 작성합니다.";
    }

    private String confidenceLevel(Double confidence) {
        if (confidence == null) {
            return "LOW";
        }
        if (confidence >= 0.8) {
            return "HIGH";
        }
        if (confidence >= 0.5) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private record MappingRow(
            Long id,
            Long tabId,
            String sourceLabel,
            String semanticFieldKey,
            String fieldKey,
            String displayName,
            Double confidence,
            Timestamp createdAt
    ) {
    }
}
