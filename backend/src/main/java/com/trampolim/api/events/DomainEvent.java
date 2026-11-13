package com.trampolim.api.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Versioned, privacy-minimized event persisted in the same trail transaction as its aggregate. */
public record DomainEvent(UUID id, String trail, String name, int schemaVersion,
        String aggregateType, String aggregateId, String idempotencyKey,
        Instant occurredAt, String correlationId, Map<String, Object> payload) {
    public DomainEvent {
        if (id == null || occurredAt == null || schemaVersion < 1
                || trail == null || !trail.matches("OPORTUNIDADES|EMPRESAS|CAPACITA")
                || name == null || !name.matches("[a-z][a-z0-9]*(\\.[a-z][a-z0-9_]*)+\\.v[1-9][0-9]*")
                || aggregateType == null || aggregateType.isBlank() || aggregateId == null || aggregateId.isBlank()
                || idempotencyKey == null || idempotencyKey.isBlank() || payload == null) {
            throw new IllegalArgumentException("Invalid domain event envelope");
        }
        payload = Map.copyOf(payload);
    }
}
