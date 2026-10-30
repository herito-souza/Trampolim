package com.trampolim.api.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.slf4j.MDC;

class OperationalRequestFilterTest {
    @Test void protectsMetricsAndClearsCorrelation() throws Exception {
        var filter = new OperationalRequestFilter("test-monitoring-key-with-at-least-32-bytes");
        var request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        var denied = new MockHttpServletResponse();
        filter.doFilter(request, denied, (a,b) -> fail("Metrics must not be exposed"));
        assertEquals(403, denied.getStatus());
        assertNotNull(denied.getHeader("X-Request-ID"));
        assertNull(MDC.get("requestId"));
        request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.addHeader("X-Monitoring-Key", "test-monitoring-key-with-at-least-32-bytes");
        var allowed = new MockHttpServletResponse();
        filter.doFilter(request, allowed, (a,b) -> assertNotNull(MDC.get("requestId")));
        assertEquals(200, allowed.getStatus());
        assertNull(MDC.get("requestId"));
    }
    @Test void failsClosedWhenUnconfiguredButLeavesHealthPublic() throws Exception {
        var filter = new OperationalRequestFilter("");
        var denied = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/actuator/prometheus"), denied, (a,b) -> fail());
        assertEquals(403, denied.getStatus());
        var health = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/actuator/health/liveness"), health, (a,b) -> {});
        assertEquals(200, health.getStatus());
    }
}
