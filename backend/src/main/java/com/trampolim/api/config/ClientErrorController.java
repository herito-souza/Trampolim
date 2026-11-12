package com.trampolim.api.config;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import com.trampolim.api.security.BoundedRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/monitoring")
@Slf4j
public class ClientErrorController {
    private final BoundedRateLimiter rates;
    public ClientErrorController() { this(new BoundedRateLimiter()); }
    ClientErrorController(BoundedRateLimiter rates) { this.rates = rates; }
    public record ClientErrorInput(String eventType, String path) {}
    @PostMapping("/client-errors")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@RequestBody ClientErrorInput input, HttpServletRequest request) {
        if (input == null || !java.util.List.of("runtime", "unhandled-rejection").contains(input.eventType())
                || input.path() == null || input.path().length() > 120 || !input.path().matches("^(/[A-Za-z0-9/_-]*)?$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (!rates.allow(request.getRemoteAddr(), 60, Duration.ofHours(1)))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        // Client paths may contain private identifiers. Correlate using the server-generated MDC ID.
        log.warn("frontend_client_error eventType={}", input.eventType());
    }
}
