package com.trampolim.api.education;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Copies the shared education catalog to Capacita while leaving the source and student progress untouched. */
@Service
@ConditionalOnProperty(name = "trampolim.datasource.capacita.enabled", havingValue = "true")
public class LegacyEducationMigrationService {
    private final JdbcTemplate legacy;
    private final JdbcTemplate capacita;
    private final TransactionTemplate targetTransaction;

    public LegacyEducationMigrationService(@Qualifier("jdbcTemplate") JdbcTemplate legacy,
            @Qualifier("capacitaJdbcTemplate") JdbcTemplate capacita,
            @Qualifier("capacitaTransactionManager") PlatformTransactionManager transactionManager) {
        this.legacy = legacy;
        this.capacita = capacita;
        this.targetTransaction = new TransactionTemplate(transactionManager);
    }

    public record Preview(long sourceTracks, long importedTracks, long sourceCategories, long importedCategories,
            long sourceContents, long importedContents, long legacyProgressRows) {}
    public record MigrationResult(long importedTracks, long importedCategories, long importedContents,
            long legacyProgressRowsPreserved) {}

    public Preview preview() {
        return new Preview(sourceCount("education_tracks"), count(capacita, "SELECT COUNT(*) FROM education_tracks WHERE legacy_track_id IS NOT NULL"),
                sourceCount("education_categories"), count(capacita, "SELECT COUNT(*) FROM education_categories WHERE legacy_category_id IS NOT NULL"),
                sourceCount("education_contents"), count(capacita, "SELECT COUNT(*) FROM education_contents WHERE legacy_content_id IS NOT NULL"),
                sourceCount("user_education_progress"));
    }

    /** Idempotent and target-transactional. No accounts, credentials, private profile data, or progress are copied. */
    public MigrationResult migrate() {
        if (!sourceTableExists("education_tracks"))
            return new MigrationResult(0, 0, 0, sourceCount("user_education_progress"));
        return targetTransaction.execute(status -> {
            long tracksBefore = count(capacita, "SELECT COUNT(*) FROM education_tracks WHERE legacy_track_id IS NOT NULL");
            long categoriesBefore = count(capacita, "SELECT COUNT(*) FROM education_categories WHERE legacy_category_id IS NOT NULL");
            long contentsBefore = count(capacita, "SELECT COUNT(*) FROM education_contents WHERE legacy_content_id IS NOT NULL");

            List<Map<String, Object>> sourceTracks = legacy.queryForList("SELECT id,code,title,description,sort_order FROM education_tracks ORDER BY id");
            for (Map<String, Object> track : sourceTracks) {
                long sourceTrackId = number(track, "id");
                Long targetTrackId = capacita.queryForObject("""
                        INSERT INTO education_tracks(code,title,description,sort_order,legacy_track_id)
                        VALUES (?,?,?,?,?)
                        ON CONFLICT(code) DO UPDATE SET legacy_track_id=COALESCE(education_tracks.legacy_track_id,EXCLUDED.legacy_track_id)
                        RETURNING id
                        """, Long.class, value(track, "code"), value(track, "title"), value(track, "description"), number(track, "sort_order"), sourceTrackId);

                List<Map<String, Object>> sourceCategories = legacy.queryForList(
                        "SELECT id,code,name,description,sort_order FROM education_categories WHERE track_id=? ORDER BY id", sourceTrackId);
                for (Map<String, Object> category : sourceCategories) {
                    long sourceCategoryId = number(category, "id");
                    Long targetCategoryId = capacita.queryForObject("""
                            INSERT INTO education_categories(track_id,code,name,description,sort_order,legacy_category_id)
                            VALUES (?,?,?,?,?,?)
                            ON CONFLICT(track_id,code) DO UPDATE SET legacy_category_id=COALESCE(education_categories.legacy_category_id,EXCLUDED.legacy_category_id)
                            RETURNING id
                            """, Long.class, targetTrackId, value(category, "code"), value(category, "name"),
                            value(category, "description"), number(category, "sort_order"), sourceCategoryId);
                    importContents(sourceCategoryId, targetCategoryId);
                }
            }

            return new MigrationResult(
                    count(capacita, "SELECT COUNT(*) FROM education_tracks WHERE legacy_track_id IS NOT NULL") - tracksBefore,
                    count(capacita, "SELECT COUNT(*) FROM education_categories WHERE legacy_category_id IS NOT NULL") - categoriesBefore,
                    count(capacita, "SELECT COUNT(*) FROM education_contents WHERE legacy_content_id IS NOT NULL") - contentsBefore,
                    sourceCount("user_education_progress"));
        });
    }

    private void importContents(long sourceCategoryId, long targetCategoryId) {
        List<Map<String, Object>> rows = legacy.queryForList("""
                SELECT id,title,summary,difficulty,resource_type,source_url,is_free,has_certificate,duration_minutes,
                       status,created_by,updated_by,published_at,created_at,updated_at
                FROM education_contents WHERE category_id=? ORDER BY id
                """, sourceCategoryId);
        for (Map<String, Object> content : rows) {
            Object rawStatus = content.get("status");
            String status = rawStatus instanceof String text && List.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(text) ? text : "DRAFT";
            capacita.update("""
                    INSERT INTO education_contents(category_id,title,summary,difficulty,resource_type,source_url,is_free,
                        has_certificate,duration_minutes,status,created_by,updated_by,published_at,created_at,updated_at,legacy_content_id)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT(legacy_content_id) WHERE legacy_content_id IS NOT NULL DO NOTHING
                    """, targetCategoryId, value(content, "title"), value(content, "summary"), nullableValue(content, "difficulty"),
                    value(content, "resource_type"), nullableValue(content, "source_url"), bool(content, "is_free"),
                    bool(content, "has_certificate"), nullableNumber(content, "duration_minutes"), status,
                    nullableValue(content, "created_by"), nullableValue(content, "updated_by"), content.get("published_at"),
                    content.get("created_at"), content.get("updated_at"), number(content, "id"));
        }
    }

    private long sourceCount(String table) {
        return sourceTableExists(table) ? count(legacy, "SELECT COUNT(*) FROM " + table) : 0;
    }
    private boolean sourceTableExists(String table) {
        Boolean exists = legacy.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class, "public." + table);
        return Boolean.TRUE.equals(exists);
    }
    private long count(JdbcTemplate jdbc, String sql) { Long value = jdbc.queryForObject(sql, Long.class); return value == null ? 0 : value; }
    private long number(Map<String, Object> row, String key) { Object value = row.get(key); return value instanceof Number number ? number.longValue() : 0; }
    private Integer nullableNumber(Map<String, Object> row, String key) { Object value = row.get(key); return value instanceof Number number ? number.intValue() : null; }
    private boolean bool(Map<String, Object> row, String key) { return Boolean.TRUE.equals(row.get(key)); }
    private String value(Map<String, Object> row, String key) { Object value = row.get(key); return value == null ? "" : value.toString(); }
    private String nullableValue(Map<String, Object> row, String key) { Object value = row.get(key); return value == null ? null : value.toString(); }
}
