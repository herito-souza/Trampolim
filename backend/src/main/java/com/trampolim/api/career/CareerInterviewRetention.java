package com.trampolim.api.career;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Removes unfinished interview drafts when the user did not opt in to history. */
@Component
public class CareerInterviewRetention {
    private final JdbcTemplate jdbc;
    public CareerInterviewRetention(@Qualifier("jdbcTemplate") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Scheduled(fixedDelayString = "${career.interviews.cleanup-ms:3600000}")
    public void cleanExpiredDrafts() {
        try {
            jdbc.update("DELETE FROM career_interview_sessions WHERE save_history=FALSE AND created_at<CURRENT_TIMESTAMP-INTERVAL '24 hours'");
        } catch (org.springframework.dao.DataAccessException unavailable) {
            // A later scheduled pass retries; never expose or log interview text.
        }
    }
}
