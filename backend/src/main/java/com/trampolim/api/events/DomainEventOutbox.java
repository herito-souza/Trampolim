package com.trampolim.api.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

/** Writes into the outbox owned by the caller's current trail transaction. */
@Component
public class DomainEventOutbox {
    private final ObjectMapper json;

    public DomainEventOutbox(ObjectMapper json) { this.json = json; }

    public UUID append(JdbcTemplate jdbc, String trail, String name, String aggregateType,
            String aggregateId, String idempotencyKey, Map<String, Object> payload) {
        return append(jdbc, trail, name, aggregateType, aggregateId, idempotencyKey, null, payload);
    }

    public UUID append(JdbcTemplate jdbc, String trail, String name, String aggregateType,
            String aggregateId, String idempotencyKey, String correlationId, Map<String, Object> payload) {
        UUID id = UUID.randomUUID();
        String resolvedCorrelation = correlationId == null ? MDC.get("correlationId") : correlationId;
        DomainEvent event = new DomainEvent(id, trail, name, 1, aggregateType, aggregateId,
                idempotencyKey, Instant.now(), resolvedCorrelation, payload);
        try {
            jdbc.update("""
                    INSERT INTO domain_event_outbox
                      (event_id,trail,event_name,schema_version,aggregate_type,aggregate_id,idempotency_key,occurred_at,correlation_id,payload)
                    VALUES (?,?,?,?,?,?,?,?,?,?::jsonb)
                    ON CONFLICT (idempotency_key) DO NOTHING
                    """, event.id(), event.trail(), event.name(), event.schemaVersion(), event.aggregateType(),
                    event.aggregateId(), event.idempotencyKey(), java.sql.Timestamp.from(event.occurredAt()),
                    event.correlationId(), json.writeValueAsString(event.payload()));
            return id;
        } catch (JsonProcessingException invalidPayload) {
            throw new IllegalArgumentException("Could not serialize domain event payload", invalidPayload);
        }
    }
}
