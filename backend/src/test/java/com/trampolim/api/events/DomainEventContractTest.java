package com.trampolim.api.events;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainEventContractTest {
    @Test void acceptsVersionedEventEnvelope() {
        var event = new DomainEvent(UUID.randomUUID(), "EMPRESAS", "application.created.v1", 1,
                "application", "42", "application-created:42", Instant.now(), null,
                Map.of("applicationId", 42L));
        assertEquals("application.created.v1", event.name());
        assertEquals(1, event.schemaVersion());
        assertThrows(UnsupportedOperationException.class, () -> event.payload().put("email", "private@example.test"));
    }

    @Test void rejectsUnversionedOrCrossTrailEnvelope() {
        assertThrows(IllegalArgumentException.class, () -> new DomainEvent(UUID.randomUUID(), "EMPRESAS",
                "ApplicationCreated", 1, "application", "42", "key", Instant.now(), null, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new DomainEvent(UUID.randomUUID(), "ALL",
                "application.created.v1", 1, "application", "42", "key", Instant.now(), null, Map.of()));
    }
}
