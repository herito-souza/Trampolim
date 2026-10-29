package com.trampolim.api.config;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class HealthController {
    private final DataSource dataSource;
    private final ObjectProvider<DataSource> empresasDataSource;
    private final ObjectProvider<DataSource> capacitaDataSource;
    @Value("${trampolim.datasource.empresas.enabled:true}") private boolean empresasEnabled;
    @Value("${trampolim.datasource.capacita.enabled:true}") private boolean capacitaEnabled;

    public HealthController(DataSource dataSource,
            @Qualifier("empresasDataSource") ObjectProvider<DataSource> empresasDataSource,
            @Qualifier("capacitaDataSource") ObjectProvider<DataSource> capacitaDataSource) {
        this.dataSource = dataSource;
        this.empresasDataSource = empresasDataSource;
        this.capacitaDataSource = capacitaDataSource;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        String oportunidades = probe(dataSource, "oportunidades");
        String empresas = secondaryProbe(empresasDataSource.getIfAvailable(), empresasEnabled, "empresas");
        String capacita = secondaryProbe(capacitaDataSource.getIfAvailable(), capacitaEnabled, "capacita");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP".equals(oportunidades)
                ? "UP".equals(empresas) && "UP".equals(capacita) ? "UP" : "DEGRADED"
                : "DOWN");
        body.put("trails", Map.of("oportunidades", oportunidades, "empresas", empresas, "capacita", capacita));
        if (!"UP".equals(oportunidades)) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        return ResponseEntity.ok(body);
    }

    private String secondaryProbe(DataSource source, boolean enabled, String trail) {
        if (!enabled || source == null) return "DISABLED";
        return probe(source, trail);
    }

    private String probe(DataSource source, String trail) {
        try (var connection = source.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (SQLException | RuntimeException error) {
            log.warn("health_trail_database_unavailable trail={} exceptionType={}", trail, error.getClass().getSimpleName());
            return "DOWN";
        }
    }
}
