package com.trampolim.api.communication;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
class CommunicationWorkerMetricsTest {
 private CommunicationStore store(){return new CommunicationStore(null,null,"CAPACITA"){@Override public QueueSnapshot queueSnapshot(){return new QueueSnapshot(7,2,45);}};}
 @Test void samplesQueuesWithoutDatabaseQueriesDuringScrape(){var metrics=new SimpleMeterRegistry();var worker=new CommunicationWorker(List.of(store()),metrics);assertTrue(Double.isNaN(metrics.get("communication.queue.pending").gauge().value()));worker.sampleQueues();assertEquals(7,metrics.get("communication.queue.pending").gauge().value());assertEquals(45,metrics.get("communication.queue.oldest.due.seconds").gauge().value());assertEquals(1,metrics.get("communication.queue.up").gauge().value());}
 @Test void failedSamplingIsUnknownRatherThanEmpty(){var store=spy(store());var metrics=new SimpleMeterRegistry();var worker=new CommunicationWorker(List.of(store),metrics);worker.sampleQueues();doThrow(new IllegalStateException("private detail")).when(store).queueSnapshot();worker.sampleQueues();assertEquals(0,metrics.get("communication.queue.up").gauge().value());assertTrue(Double.isNaN(metrics.get("communication.queue.pending").gauge().value()));}
}
