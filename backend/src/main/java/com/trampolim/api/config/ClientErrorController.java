package com.trampolim.api.config;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Slf4j
public class ClientErrorController {
    private final ConcurrentHashMap<String, RateWindow> reportRates = new ConcurrentHashMap<>();
    public record ClientErrorInput(String eventType, String path) {}
    private record RateWindow(Instant expiresAt, int count) {}

    @PostMapping("/client-errors")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@RequestBody ClientErrorInput input, HttpServletRequest request) {
        if (input == null || !java.util.List.of("runtime", "unhandled-rejection").contains(input.eventType())
                || input.path() == null || input.path().length() > 120 || !input.path().matches("^(/[A-Za-z0-9/_-]*)?$") )
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        Instant now = Instant.now();
        RateWindow window = reportRates.compute(request.getRemoteAddr(), (key, previous) -> previous == null || previous.expiresAt().isBefore(now)
                ? new RateWindow(now.plus(1, ChronoUnit.HOURS), 1) : new RateWindow(previous.expiresAt(), previous.count() + 1));
        if (reportRates.size() > 10_000) reportRates.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        if (window.count() > 60) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        log.warn("frontend_client_error eventType={} path={}", input.eventType(), input.path());
    }
}
