package com.trampolim.api.future;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import io.micrometer.core.instrument.*;
/** Durable work in bounded batches; no email, external messages or candidate identity transfer. */
@Component
public class FutureTalentWorker {
 private final java.util.concurrent.atomic.AtomicBoolean running=new java.util.concurrent.atomic.AtomicBoolean();
 private final java.util.concurrent.ExecutorService executor=java.util.concurrent.Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"future-talent-worker");thread.setDaemon(true);return thread;});
 private final FutureTalentService service;private final Counter failures;private final Timer duration;
 @Value("${future.poll-enabled:true}")private boolean enabled;
 public FutureTalentWorker(FutureTalentService service,MeterRegistry registry){this.service=service;failures=registry.counter("future.talent.poll.failures");duration=registry.timer("future.talent.poll.duration");}
 @Scheduled(initialDelayString="${future.poll-initial-delay-ms:30000}",fixedDelayString="${future.poll-delay-ms:60000}")public void poll(){
  // Never block the shared scheduler used by educational notifications. Only one batch can be queued/running.
  if(!enabled||!running.compareAndSet(false,true))return;
  try{executor.execute(()->{try{runBatch();}finally{running.set(false);}});}catch(java.util.concurrent.RejectedExecutionException error){running.set(false);}
 }
 void runBatch(){duration.record(()->{try{service.cleanup();}catch(RuntimeException error){failures.increment();}try{service.pollVacancies();service.deliverBatch();service.deliverBatch();service.refreshBatch();}catch(RuntimeException error){failures.increment();}});}
 @jakarta.annotation.PreDestroy public void close(){executor.shutdownNow();}
}
