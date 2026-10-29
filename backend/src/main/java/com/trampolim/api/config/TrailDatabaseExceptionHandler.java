package com.trampolim.api.config;

import java.util.Map;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Avoid leaking database details and make an unavailable trail database explicit to clients. */
@RestControllerAdvice
public class TrailDatabaseExceptionHandler {
    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<Map<String, String>> databaseUnavailable(DataAccessResourceFailureException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "O serviço desta área está temporariamente indisponível."));
    }
}
