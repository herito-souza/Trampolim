package com.trampolim.api.education;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class PublicLearningSummaryController implements PublicLearningCatalog {
    private final JdbcTemplate education;
    public PublicLearningSummaryController(@Qualifier("capacitaJdbcTemplate") JdbcTemplate education) { this.education=education; }
    @Override @GetMapping("/api/capacita/public-summary")
    public Summary summary() {
        try {
        Long count=education.queryForObject("SELECT COUNT(*) FROM education_contents WHERE status='PUBLISHED'",Long.class);
        return new Summary(count==null?0L:count);
        } catch (org.springframework.dao.DataAccessResourceFailureException unavailable) {
            return new Summary(null);
        }
    }
}
