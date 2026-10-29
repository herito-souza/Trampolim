package com.trampolim.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

class TrailDatabaseExceptionHandlerTest {
    @Test
    void connectionFailuresReturnUnavailableWithoutExposingDatabaseDetails() {
        var response = new TrailDatabaseExceptionHandler().databaseUnavailable(
                new DataAccessResourceFailureException("connection to private-db.internal:5432 failed"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertFalse(response.getBody().toString().contains("private-db.internal"));
    }
}
