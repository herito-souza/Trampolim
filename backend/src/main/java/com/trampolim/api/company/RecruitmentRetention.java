package com.trampolim.api.company;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Bounded cleanup of new recruitment data only; does not delete applications or hiring evidence. */
@Component
@ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class RecruitmentRetention {
    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final int historyDays;
    public RecruitmentRetention(@Qualifier("empresasJdbcTemplate") JdbcTemplate jdbc,
            @Value("${recruitment.retention.enabled:true}") boolean enabled,
            @Value("${recruitment.history-retention-days:730}") int historyDays){
        if(historyDays<30||historyDays>3650)throw new IllegalArgumentException("Invalid recruitment history retention");
        this.jdbc=jdbc;this.enabled=enabled;this.historyDays=historyDays;
    }
    @Scheduled(fixedDelayString="${recruitment.retention.interval-ms:3600000}",initialDelayString="${recruitment.retention.initial-delay-ms:60000}")
    @Transactional("empresasTransactionManager")
    public void purge(){
        if(!enabled)return;
        // Tombstones retain idempotent request linkage without retaining note text.
        jdbc.update("""
          UPDATE company_recruitment_notes SET body=NULL,deleted_at=COALESCE(deleted_at,CURRENT_TIMESTAMP)
          WHERE id IN (SELECT id FROM company_recruitment_notes WHERE expires_at<=CURRENT_TIMESTAMP AND body IS NOT NULL
            ORDER BY expires_at,id LIMIT 1000 FOR UPDATE SKIP LOCKED)
          """);
        jdbc.update("""
          DELETE FROM company_recruitment_events WHERE id IN (
            SELECT id FROM company_recruitment_events WHERE revision>0 AND created_at<CURRENT_TIMESTAMP-make_interval(days=>?)
            ORDER BY created_at,id LIMIT 1000 FOR UPDATE SKIP LOCKED)
          """,historyDays);
        jdbc.update("""
          DELETE FROM company_recruitment_notes WHERE id IN (
            SELECT id FROM company_recruitment_notes WHERE body IS NULL
             AND created_at<CURRENT_TIMESTAMP-make_interval(days=>?)
            ORDER BY created_at,id LIMIT 1000 FOR UPDATE SKIP LOCKED)
          """,historyDays);
    }
}
