package com.trampolim.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;
import java.sql.Connection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class HealthControllerTest {
    @Test
    @SuppressWarnings("unchecked")
    void reportsEachTrailAndDegradedStatusWhenASideDatabaseIsUnavailable() throws Exception {
        DataSource primary = source(true);
        DataSource companies = source(false);
        DataSource students = source(true);
        ObjectProvider<DataSource> companyProvider = provider(companies);
        ObjectProvider<DataSource> studentProvider = provider(students);
        HealthController controller = new HealthController(primary, companyProvider, studentProvider);
        ReflectionTestUtils.setField(controller, "empresasEnabled", true);
        ReflectionTestUtils.setField(controller, "capacitaEnabled", true);

        var response = controller.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("DEGRADED", response.getBody().get("status"));
        assertEquals("UP", ((java.util.Map<String, String>) response.getBody().get("trails")).get("oportunidades"));
        assertEquals("DOWN", ((java.util.Map<String, String>) response.getBody().get("trails")).get("empresas"));
        assertEquals("UP", ((java.util.Map<String, String>) response.getBody().get("trails")).get("capacita"));
    }

    @Test
    void returnsServiceUnavailableWhenThePrimaryDatabaseIsDown() throws Exception {
        HealthController controller = new HealthController(source(false), provider(null), provider(null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, controller.health().getStatusCode());
    }

    private DataSource source(boolean valid) throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(valid);
        return source;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<DataSource> provider(DataSource source) {
        ObjectProvider<DataSource> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(source);
        return provider;
    }
}
