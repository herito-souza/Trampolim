package com.trampolim.api.config;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
class TrailDatabaseIsolationGuardTest {
    @Test void detectsSameDatabaseDespiteDifferentSchemaOptionsOrLoopbackAliases() {
        var env = new MockEnvironment().withProperty("spring.datasource.url", "jdbc:postgresql://localhost/main?currentSchema=one")
                .withProperty("trampolim.datasource.empresas.enabled", "true")
                .withProperty("trampolim.datasource.empresas.url", "jdbc:postgresql://127.0.0.1:5432/main?currentSchema=two");
        assertThrows(IllegalStateException.class, () -> new TrailDatabaseIsolationGuard(env).afterPropertiesSet());
        env.setProperty("trampolim.datasource.empresas.url", "jdbc:postgresql://localhost/companies");
        assertDoesNotThrow(() -> new TrailDatabaseIsolationGuard(env).afterPropertiesSet());
    }
    @Test void distinctServersAndDatabasesAreAllowed() {
        assertNotEquals(TrailDatabaseIsolationGuard.identity("jdbc:postgresql://server-a/db"), TrailDatabaseIsolationGuard.identity("jdbc:postgresql://server-b/db"));
        assertThrows(IllegalStateException.class, () -> TrailDatabaseIsolationGuard.identity("invalid"));
    }
}
