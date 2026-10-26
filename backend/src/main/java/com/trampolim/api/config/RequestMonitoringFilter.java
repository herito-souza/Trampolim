package com.trampolim.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestMonitoringFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestMonitoringFilter.class);
    private static final long SLOW_REQUEST_MILLIS = 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 12);
        String path = request.getRequestURI().replaceAll("[^A-Za-z0-9/_-]", "_");
        if (path.length() > 120) path = path.substring(0, 120);
        long startedAt = System.nanoTime();
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-ID", requestId);
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException error) {
            log.error("http_request_failed requestId={} method={} path={} errorType={}", requestId, request.getMethod(), path, error.getClass().getSimpleName());
            throw error;
        } finally {
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
            if (response.getStatus() >= 500) {
                log.warn("http_request_server_error requestId={} method={} path={} status={} durationMs={}", requestId, request.getMethod(), path, response.getStatus(), durationMillis);
            } else if (durationMillis >= SLOW_REQUEST_MILLIS) {
                log.info("http_request_slow requestId={} method={} path={} status={} durationMs={}", requestId, request.getMethod(), path, response.getStatus(), durationMillis);
            }
            MDC.remove("requestId");
        }
    }
}
