package com.trampolim.api.events;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

/** Local durable relay. The initial idempotent consumer is the per-trail event receipt ledger. */
public class DomainEventRelay {
    private static final Logger log = LoggerFactory.getLogger(DomainEventRelay.class);
    private static final String CONSUMER = "domain-event-ledger.v1";
    private final String trail;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final int retentionDays;

    public DomainEventRelay(String trail, JdbcTemplate jdbc, TransactionTemplate transaction, int retentionDays) {
        if (retentionDays < 30 || retentionDays > 3650) throw new IllegalArgumentException("Domain event retention must be 30-3650 days");
        this.trail = trail;
        this.jdbc = jdbc;
        this.transaction = transaction;
        this.retentionDays = retentionDays;
    }

    @Scheduled(initialDelayString = "${domain-events.initial-delay-ms:5000}", fixedDelayString = "${domain-events.poll-delay-ms:1000}")
    public void pump() {
        for (int index = 0; index < 25; index++) {
            try {
                Boolean handled = transaction.execute(status -> processOne());
                if (!Boolean.TRUE.equals(handled)) return;
            } catch (RelayFailure failure) {
                log.warn("Domain event relay failed for trail {} ({})", trail, failure.getCause().getClass().getSimpleName());
                pumpFailure(failure.eventId(), failure.getCause().getClass().getSimpleName());
                return;
            } catch (RuntimeException failure) {
                log.warn("Domain event relay unavailable for trail {} ({})", trail, failure.getClass().getSimpleName());
                return;
            }
        }
    }

    @Scheduled(cron = "${domain-events.retention-cron:0 17 3 * * *}")
    public void retain() {
        transaction.executeWithoutResult(status -> jdbc.update("""
                DELETE FROM domain_event_outbox WHERE event_id IN (
                  SELECT event_id FROM domain_event_outbox
                  WHERE state IN ('PROCESSED','FAILED') AND occurred_at<CURRENT_TIMESTAMP-make_interval(days=>?)
                  ORDER BY occurred_at,event_id LIMIT 500
                )
                """, retentionDays));
    }

    private boolean processOne() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT event_id FROM domain_event_outbox
                WHERE state='PENDING' AND next_attempt_at<=CURRENT_TIMESTAMP
                ORDER BY occurred_at,event_id LIMIT 1 FOR UPDATE SKIP LOCKED
                """);
        if (rows.isEmpty()) return false;
        UUID id = (UUID) rows.getFirst().get("event_id");
        try {
            jdbc.update("""
                    INSERT INTO domain_event_receipts(event_id,consumer_name,processed_at)
                    VALUES (?, ?, CURRENT_TIMESTAMP) ON CONFLICT(event_id,consumer_name) DO NOTHING
                    """, id, CONSUMER);
            jdbc.update("UPDATE domain_event_outbox SET state='PROCESSED',processed_at=CURRENT_TIMESTAMP,last_error=NULL WHERE event_id=?", id);
            return true;
        } catch (RuntimeException failure) {
            throw new RelayFailure(id, failure);
        }
    }

    /** Separate transaction keeps a failed delivery retryable after processing rolls back. */
    private void pumpFailure(UUID eventId, String safeCode) {
        transaction.executeWithoutResult(status -> jdbc.update("""
                UPDATE domain_event_outbox SET attempts=attempts+1,
                  state=CASE WHEN attempts+1>=10 THEN 'FAILED' ELSE 'PENDING' END,
                  last_error=?, next_attempt_at=CURRENT_TIMESTAMP + make_interval(secs => LEAST(3600, 5 * power(2, LEAST(attempts,9)))::int)
                WHERE event_id=? AND state='PENDING'
                """, safeCode, eventId));
    }

    private static final class RelayFailure extends RuntimeException {
        private final UUID eventId;
        private RelayFailure(UUID eventId, RuntimeException cause) { super(cause); this.eventId = eventId; }
        private UUID eventId() { return eventId; }
    }
}
