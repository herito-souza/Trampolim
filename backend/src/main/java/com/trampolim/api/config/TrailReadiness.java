package com.trampolim.api.config;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/** All enabled trails must be available; liveness intentionally excludes databases. */
@Component("trails")
public class TrailReadiness implements HealthIndicator {
 private final HealthController controller;
 public TrailReadiness(HealthController controller){this.controller=controller;}
 @Override public Health health(){
  var body=controller.health().getBody();
  if(body==null || !(body.get("trails") instanceof Map<?,?> trails)) return Health.down().build();
  boolean ready=trails.values().stream().allMatch(v->"UP".equals(v)||"DISABLED".equals(v));
  return (ready?Health.up():Health.down()).build();
 }
}
