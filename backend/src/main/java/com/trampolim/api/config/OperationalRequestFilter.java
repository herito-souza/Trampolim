package com.trampolim.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Correlation without logging URLs, query parameters, bodies, identities or credentials. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class OperationalRequestFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(OperationalRequestFilter.class);
    private final byte[] monitoringKey;
    public OperationalRequestFilter(@Value("${operations.monitoring-key:}") String key) {
        monitoringKey = key.getBytes(StandardCharsets.UTF_8);
    }
    public static boolean validMonitoringKey(String expected, String supplied) {
        return expected != null && expected.length() >= 32 && supplied != null && supplied.length() <= 512
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        response.setHeader("X-Request-ID", id);
        MDC.put("requestId", id);
        long started = System.nanoTime();
        try {
            String path = request.getRequestURI();
            if (path.equals("/actuator/prometheus") || path.startsWith("/actuator/prometheus/")) {
                String supplied = request.getHeader("X-Monitoring-Key");
                if (monitoringKey.length < 32 || supplied == null || supplied.length() > 512
                        || !MessageDigest.isEqual(monitoringKey, supplied.getBytes(StandardCharsets.UTF_8))) {
                    response.setStatus(403);
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            long elapsed = (System.nanoTime() - started) / 1_000_000;
            if (!request.isAsyncStarted() && (response.getStatus() >= 500 || elapsed >= 1000)) {
                log.warn("http_request_completed method={} status={} elapsed_ms={}", request.getMethod(), response.getStatus(), elapsed);
            }
            MDC.remove("requestId");
        }
    }
}
