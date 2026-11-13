package com.trampolim.api.config;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

/** Probe outside scrape threads. Missing samples never masquerade as healthy. */
@Component
public class OperationalDatabaseMetrics {
 private final HealthController health;
 private final Map<String,AtomicInteger> states=new ConcurrentHashMap<>();
 public OperationalDatabaseMetrics(HealthController health,MeterRegistry registry){this.health=health;for(String trail:new String[]{"oportunidades","empresas","capacita"}){var state=new AtomicInteger(-1);states.put(trail,state);registry.gauge("trampolim.database.up",java.util.List.of(io.micrometer.core.instrument.Tag.of("trail",trail)),state,AtomicInteger::doubleValue);}}
 @Scheduled(fixedDelayString="${operations.database-probe-ms:30000}") public void sample(){
  try {var body=health.health().getBody();if(body==null || !(body.get("trails") instanceof Map<?,?> trails))throw new IllegalStateException();
   states.forEach((trail,state)->state.set(switch(String.valueOf(trails.get(trail))){case "UP"->1;case "DISABLED"->-1;default->0;}));
  }catch(RuntimeException error){states.values().forEach(state->state.set(0));}
 }
}
