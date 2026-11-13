package com.trampolim.api.communication;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component @ConditionalOnProperty(name="communications.worker-enabled",havingValue="true",matchIfMissing=true)
public class CommunicationWorker {
 private final List<CommunicationStore> stores; private final MeterRegistry metrics;
 private record State(double pending,double failed,double age,double up){}
 private final ConcurrentHashMap<String,AtomicReference<State>> states=new ConcurrentHashMap<>();
 public CommunicationWorker(List<CommunicationStore> stores,MeterRegistry metrics){
  this.stores=stores;this.metrics=metrics;
  for(var store:stores){var state=new AtomicReference<>(new State(Double.NaN,Double.NaN,Double.NaN,0));states.put(store.trail,state);
   var tags=List.of(io.micrometer.core.instrument.Tag.of("trail",store.trail));
   metrics.gauge("communication.queue.pending",tags,state,v->v.get().pending());
   metrics.gauge("communication.queue.failed",tags,state,v->v.get().failed());
   metrics.gauge("communication.queue.oldest.due.seconds",tags,state,v->v.get().age());
   metrics.gauge("communication.queue.up",tags,state,v->v.get().up());
  }
 }
 @Scheduled(fixedDelayString="${communications.poll-ms:2000}") public void deliver(){for(var store:stores){var sample=Timer.start(metrics);String outcome="success";try{store.pumpSources();store.pumpDeliveries();}catch(RuntimeException error){outcome="failure";metrics.counter("communication.worker.failures","trail",store.trail).increment();}finally{sample.stop(metrics.timer("communication.worker.duration","trail",store.trail,"outcome",outcome));}}}
 @Scheduled(fixedDelayString="${communications.metrics-poll-ms:30000}") public void sampleQueues(){for(var store:stores)try{var q=store.queueSnapshot();states.get(store.trail).set(new State(q.pending(),q.failed(),q.oldestDueSeconds(),1));}catch(RuntimeException error){states.get(store.trail).set(new State(Double.NaN,Double.NaN,Double.NaN,0));}}
 @Scheduled(fixedDelayString="${communications.retention-poll-ms:3600000}")public void retain(){for(var store:stores)try{store.retain();}catch(RuntimeException error){metrics.counter("communication.retention.failures","trail",store.trail).increment();}}
}
