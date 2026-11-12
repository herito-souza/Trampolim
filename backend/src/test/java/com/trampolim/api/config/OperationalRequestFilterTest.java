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

    @Test void oneServerGeneratedIdAndNoPrivatePathOrExceptionTextInLogs() throws Exception {
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(OperationalRequestFilter.class);
        var appender=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();appender.start();logger.addAppender(appender);
        try {var request=new MockHttpServletRequest("GET","/api/shared/opaque-secret-ticket");request.addHeader("X-Request-ID","untrusted-id");var response=new MockHttpServletResponse();
            assertThrows(jakarta.servlet.ServletException.class,()->new OperationalRequestFilter("").doFilter(request,response,(a,b)->{assertEquals(response.getHeader("X-Request-ID"),MDC.get("requestId"));throw new jakarta.servlet.ServletException("sensitive internal error");}));
            assertNotEquals("untrusted-id",response.getHeader("X-Request-ID"));assertNull(MDC.get("requestId"));assertFalse(appender.list.isEmpty());for(var event:appender.list){assertFalse(event.getFormattedMessage().contains("opaque-secret-ticket"));assertFalse(event.getFormattedMessage().contains("sensitive internal error"));assertEquals(response.getHeader("X-Request-ID"),event.getMDCPropertyMap().get("requestId"));}
        } finally {logger.detachAppender(appender);appender.stop();}
    }
}
