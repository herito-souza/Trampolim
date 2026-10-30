package com.trampolim.api.education;
import java.util.*;import com.trampolim.api.radar.RadarBridge;
import org.springframework.stereotype.Service;import org.springframework.beans.factory.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.scheduling.annotation.Scheduled;
/** Durable invalidation, bounded batches and lease; private evidence is revalidated even during outages. */
@Service @ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class PassportOutbox {
 private final java.util.concurrent.atomic.AtomicBoolean running=new java.util.concurrent.atomic.AtomicBoolean();
 private final java.util.concurrent.ExecutorService executor=java.util.concurrent.Executors.newSingleThreadExecutor(task->{var thread=new Thread(task,"passport-delivery");thread.setDaemon(true);return thread;});
 private final JdbcTemplate jdbc;private final RadarBridge bridge;
 @Value("${passport.delivery-enabled:true}")private boolean enabled;
 public PassportOutbox(@Qualifier("capacitaJdbcTemplate")JdbcTemplate jdbc,RadarBridge bridge){this.jdbc=jdbc;this.bridge=bridge;}
 public void enqueue(UUID receipt){jdbc.update("INSERT INTO learning_radar_invalidation_outbox(receipt_id) VALUES (?) ON CONFLICT(receipt_id) DO UPDATE SET version=learning_radar_invalidation_outbox.version+1,next_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP",receipt);}
 public void invalidateAward(UUID award){for(UUID receipt:jdbc.queryForList("UPDATE learning_radar_exports SET status='REVOKED',updated_at=CURRENT_TIMESTAMP WHERE award_id=? AND status<>'REVOKED' RETURNING id",UUID.class,award))enqueue(receipt);}
 @Scheduled(fixedDelayString="${passport.delivery-delay-ms:30000}")public void scheduled(){if(!enabled||!running.compareAndSet(false,true))return;try{executor.execute(()->{try{deliver();}finally{running.set(false);}});}catch(java.util.concurrent.RejectedExecutionException closed){running.set(false);}}
 @jakarta.annotation.PreDestroy public void close(){executor.shutdownNow();}
 public int deliver(){
  jdbc.update("WITH expired AS (UPDATE learning_radar_exports e SET status='REVOKED',updated_at=CURRENT_TIMESTAMP WHERE e.id IN (SELECT x.id FROM learning_radar_exports x JOIN learning_competency_awards a ON a.id=x.award_id WHERE x.status<>'REVOKED' AND (a.revoked_at IS NOT NULL OR a.valid_until<=CURRENT_TIMESTAMP) ORDER BY x.consent_at LIMIT 20) RETURNING e.id) INSERT INTO learning_radar_invalidation_outbox(receipt_id) SELECT id FROM expired ON CONFLICT(receipt_id) DO UPDATE SET version=learning_radar_invalidation_outbox.version+1,next_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP");
  var leased=jdbc.queryForList("UPDATE learning_radar_invalidation_outbox SET next_at=CURRENT_TIMESTAMP+INTERVAL '2 minutes' WHERE receipt_id IN (SELECT receipt_id FROM learning_radar_invalidation_outbox WHERE version>delivered_version AND next_at<=CURRENT_TIMESTAMP ORDER BY next_at LIMIT 20 FOR UPDATE SKIP LOCKED) RETURNING receipt_id,version");int done=0;
  for(var row:leased){if(Thread.currentThread().isInterrupted())return done;try{bridge.post("/integrations/radar/invalidate",Map.of("receipts",List.of(row.get("receipt_id"))));jdbc.update("UPDATE learning_radar_invalidation_outbox SET delivered_version=GREATEST(delivered_version,?),attempts=0,updated_at=CURRENT_TIMESTAMP WHERE receipt_id=?",row.get("version"),row.get("receipt_id"));done++;}catch(org.springframework.web.server.ResponseStatusException unavailable){jdbc.update("UPDATE learning_radar_invalidation_outbox SET attempts=LEAST(attempts+1,20),next_at=CURRENT_TIMESTAMP+make_interval(secs=>LEAST(3600,30*(attempts+1))),updated_at=CURRENT_TIMESTAMP WHERE receipt_id=?",row.get("receipt_id"));}}
  return done;
 }
}
