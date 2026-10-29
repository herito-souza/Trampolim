package com.trampolim.api.education;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import com.trampolim.api.security.PlatformAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@ConditionalOnProperty(name = "trampolim.datasource.capacita.enabled", havingValue = "true")
public class CapacitaController {
    private final JdbcTemplate jdbc;
    private final PlatformAccess platformAccess;

    public CapacitaController(@Qualifier("capacitaJdbcTemplate") JdbcTemplate jdbc, PlatformAccess platformAccess) {
        this.jdbc = jdbc;
        this.platformAccess = platformAccess;
    }

    public record Category(long id, String name, String description, int sortOrder, long contentsCount) {}
    public record Track(long id, String code, String title, String description, List<Category> categories) {}
    public record Content(long id, String title, String summary, String difficulty, String resourceType, String sourceUrl,
            boolean isFree, boolean hasCertificate, Integer durationMinutes, long categoryId, String categoryName, String stageCode, String status) {}
    public record ContentInput(long categoryId, String title, String summary, String difficulty, String resourceType,
            String sourceUrl, boolean isFree, boolean hasCertificate, Integer durationMinutes) {}
    public record ProgressInput(boolean completed) {}
    public record ReviewInput(String status) {}

    @GetMapping("/capacita/account/export")
    public java.util.Map<String, Object> exportAccount(@AuthenticationPrincipal String email) {
        if (email == null || email.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Entre na sua conta do Capacita.");
        var accounts = jdbc.queryForList("SELECT id,email,full_name,education_stage,institution_name,email_verified,terms_accepted_at,created_at,age_band,age_declared_at FROM student_users WHERE LOWER(email)=LOWER(?) AND account_status='ACTIVE'", email);
        if (accounts.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada.");
        var account = accounts.get(0);
        var progress = jdbc.queryForList("SELECT content_id,completed_at FROM student_education_progress WHERE student_user_id=? ORDER BY completed_at,content_id", account.get("id"));
        return java.util.Map.of("exportedAt", Instant.now().toString(), "trail", "capacita", "account", account, "progress", progress);
    }
    @GetMapping("/capacita/tracks")
    public List<Track> tracks() {
        List<Object[]> rows = jdbc.query("SELECT id,code,title,description FROM education_tracks ORDER BY sort_order,id",
                (rs, row) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)});
        return rows.stream().map(row -> {
            long id = (long) row[0];
            List<Category> categories = jdbc.query("SELECT c.id,c.name,c.description,c.sort_order,COUNT(ec.id) FROM education_categories c LEFT JOIN education_contents ec ON ec.category_id=c.id AND ec.status='PUBLISHED' WHERE c.track_id=? GROUP BY c.id ORDER BY c.sort_order,c.id",
                    (rs, index) -> new Category(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getLong(5)), id);
            return new Track(id, (String) row[1], (String) row[2], (String) row[3], categories);
        }).toList();
    }

    @GetMapping("/capacita/contents")
    public List<Content> contents(@RequestParam(required = false) String stage) {
        String sql = contentSql() + " WHERE ec.status='PUBLISHED'" + (stage == null || stage.isBlank() ? "" : " AND t.code=?") + " ORDER BY t.sort_order,c.sort_order,ec.created_at DESC";
        return stage == null || stage.isBlank() ? jdbc.query(sql, contentMapper()) : jdbc.query(sql, contentMapper(), stage);
    }

    @GetMapping("/capacita/contents/{id}")
    public Content content(@PathVariable long id) {
        return jdbc.query(contentSql() + " WHERE ec.status='PUBLISHED' AND ec.id=?", contentMapper(), id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Este conteúdo não está disponível."));
    }

    @GetMapping("/capacita/progress")
    public List<Long> progress(@AuthenticationPrincipal String email) {
        return jdbc.query("SELECT p.content_id FROM student_education_progress p JOIN student_users u ON u.id=p.student_user_id WHERE LOWER(u.email)=LOWER(?) ORDER BY p.completed_at DESC",
                (rs, row) -> rs.getLong(1), email);
    }

    @PutMapping("/capacita/progress/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@AuthenticationPrincipal String email, @PathVariable long contentId, @RequestBody ProgressInput input) {
        if (input == null || !input.completed()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a conclusão do conteúdo.");
        int inserted = jdbc.update("INSERT INTO student_education_progress(student_user_id,content_id) SELECT u.id,ec.id FROM student_users u JOIN education_contents ec ON ec.id=? AND ec.status='PUBLISHED' WHERE LOWER(u.email)=LOWER(?) ON CONFLICT(student_user_id,content_id) DO UPDATE SET completed_at=CURRENT_TIMESTAMP", contentId, email);
        if (inserted == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Este conteúdo não está disponível.");
    }

    @DeleteMapping("/capacita/progress/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uncomplete(@AuthenticationPrincipal String email, @PathVariable long contentId) {
        jdbc.update("DELETE FROM student_education_progress p USING student_users u WHERE p.student_user_id=u.id AND p.content_id=? AND LOWER(u.email)=LOWER(?)", contentId, email);
    }

    @GetMapping("/admin/capacita/contents")
    public List<Content> adminContents(@AuthenticationPrincipal String email) {
        requireAdmin(email);
        return jdbc.query(contentSql(), contentMapper());
    }

    @PostMapping("/admin/capacita/contents")
    @ResponseStatus(HttpStatus.CREATED)
    public Content create(@AuthenticationPrincipal String email, @RequestBody ContentInput input) {
        requireAdmin(email); validate(input);
        Long id = jdbc.queryForObject("INSERT INTO education_contents(category_id,title,summary,difficulty,resource_type,source_url,is_free,has_certificate,duration_minutes,created_by,updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?) RETURNING id",
                Long.class, input.categoryId(), input.title().trim(), input.summary().trim(), clean(input.difficulty()), clean(input.resourceType()).isBlank() ? "Material" : input.resourceType().trim(), clean(input.sourceUrl()), input.isFree(), input.hasCertificate(), input.durationMinutes(), email, email);
        return jdbc.queryForObject(contentSql() + " WHERE ec.id=?", contentMapper(), id);
    }

    @PutMapping("/admin/capacita/contents/{id}")
    public Content update(@AuthenticationPrincipal String email, @PathVariable long id, @RequestBody ContentInput input) {
        requireAdmin(email); validate(input);
        int changed = jdbc.update("UPDATE education_contents SET category_id=?,title=?,summary=?,difficulty=?,resource_type=?,source_url=?,is_free=?,has_certificate=?,duration_minutes=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                input.categoryId(), input.title().trim(), input.summary().trim(), clean(input.difficulty()), clean(input.resourceType()).isBlank() ? "Material" : input.resourceType().trim(), clean(input.sourceUrl()), input.isFree(), input.hasCertificate(), input.durationMinutes(), email, id);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return jdbc.queryForObject(contentSql() + " WHERE ec.id=?", contentMapper(), id);
    }

    @PutMapping("/admin/capacita/contents/{id}/review")
    public Content review(@AuthenticationPrincipal String email, @PathVariable long id, @RequestBody ReviewInput input) {
        requireAdmin(email);
        if (input == null || !List.of("PUBLISHED", "ARCHIVED", "DRAFT").contains(input.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de conteúdo inválido.");
        int changed = jdbc.update("UPDATE education_contents SET status=?,published_at=CASE WHEN ?='PUBLISHED' THEN COALESCE(published_at,CURRENT_TIMESTAMP) ELSE published_at END,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", input.status(), input.status(), email, id);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return jdbc.queryForObject(contentSql() + " WHERE ec.id=?", contentMapper(), id);
    }

    private String contentSql() { return "SELECT ec.id,ec.title,ec.summary,ec.difficulty,ec.resource_type,ec.source_url,ec.is_free,ec.has_certificate,ec.duration_minutes,c.id,c.name,t.code,ec.status FROM education_contents ec JOIN education_categories c ON c.id=ec.category_id JOIN education_tracks t ON t.id=c.track_id"; }
    private org.springframework.jdbc.core.RowMapper<Content> contentMapper() { return (rs, row) -> new Content(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getBoolean(7),rs.getBoolean(8),(Integer) rs.getObject(9),rs.getLong(10),rs.getString(11),rs.getString(12),rs.getString(13)); }
    private void validate(ContentInput input) {
        if (input == null || input.categoryId() <= 0 || blank(input.title()) || input.title().length() > 180 || blank(input.summary()) || input.summary().length() > 3000 || clean(input.resourceType()).length() > 40 || clean(input.difficulty()).length() > 40 || (input.durationMinutes() != null && input.durationMinutes() <= 0)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revise os campos do conteúdo e seus limites.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM education_categories WHERE id=?", Integer.class, input.categoryId()) == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma categoria válida.");
        if (!blank(input.sourceUrl())) try { URI uri=URI.create(input.sourceUrl().trim()); if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost()==null) throw new IllegalArgumentException(); } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O link precisa começar com http:// ou https://."); }
    }
    private void requireAdmin(String email) { if (!platformAccess.isSuperAdmin(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito à administração."); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private boolean blank(String value) { return clean(value).isBlank(); }
}
