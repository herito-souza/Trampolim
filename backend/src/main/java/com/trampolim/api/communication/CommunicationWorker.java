package com.trampolim.api.communication;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.micrometer.core.instrument.MeterRegistry;
@Component @ConditionalOnProperty(name="communications.worker-enabled",havingValue="true",matchIfMissing=true)
public class CommunicationWorker {
 private final List<CommunicationStore> stores;private final MeterRegistry metrics;
 public CommunicationWorker(List<CommunicationStore> stores,MeterRegistry metrics){this.stores=stores;this.metrics=metrics;}
 @Scheduled(fixedDelayString="${communications.poll-ms:2000}") public void deliver(){for(var store:stores)try{store.pumpSources();store.pumpDeliveries();}catch(RuntimeException error){metrics.counter("communication.worker.failures","trail",store.trail).increment();}}
 @Scheduled(fixedDelayString="${communications.retention-poll-ms:3600000}")public void retain(){for(var store:stores)try{store.retain();}catch(RuntimeException error){metrics.counter("communication.retention.failures","trail",store.trail).increment();}}
}
