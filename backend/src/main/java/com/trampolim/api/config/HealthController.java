package com.trampolim.api.config;

import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthController {
    private final DataSource dataSource;

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        try (var connection = dataSource.getConnection()) {
            if (connection.isValid(2)) return ResponseEntity.ok(Map.of("status", "UP", "database", "UP"));
        } catch (SQLException error) {
            log.warn("health_database_unavailable exceptionType={}", error.getClass().getSimpleName());
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "DOWN", "database", "DOWN"));
    }
}
