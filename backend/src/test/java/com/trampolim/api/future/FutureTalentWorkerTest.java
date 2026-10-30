package com.trampolim.api.future;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
class FutureTalentWorkerTest {
 @Test void disabledWorkerNeverContactsOtherEnvironments(){var service=mock(FutureTalentService.class);var worker=new FutureTalentWorker(service,new SimpleMeterRegistry());ReflectionTestUtils.setField(worker,"enabled",false);worker.poll();verifyNoInteractions(service);}
 @Test void enabledWorkerRunsBoundedStepsAndRecordsFailures(){var service=mock(FutureTalentService.class);var registry=new SimpleMeterRegistry();var worker=new FutureTalentWorker(service,registry);ReflectionTestUtils.setField(worker,"enabled",true);worker.runBatch();verify(service).pollVacancies();verify(service,times(2)).deliverBatch();verify(service).refreshBatch();verify(service).cleanup();doThrow(new IllegalStateException("Synthetic outage")).when(service).pollVacancies();assertDoesNotThrow(worker::runBatch);verify(service,times(2)).cleanup();assertEquals(1d,registry.get("future.talent.poll.failures").counter().count());}
 @Test void slowIntegrationNeverBlocksSharedSchedulerOrQueuesOverlappingBatches()throws Exception{var service=mock(FutureTalentService.class);var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);var finished=new java.util.concurrent.CountDownLatch(1);doAnswer(call->{entered.countDown();release.await(5,java.util.concurrent.TimeUnit.SECONDS);return null;}).when(service).pollVacancies();doAnswer(call->{finished.countDown();return null;}).when(service).refreshBatch();var worker=new FutureTalentWorker(service,new SimpleMeterRegistry());ReflectionTestUtils.setField(worker,"enabled",true);try{worker.poll();assertTrue(entered.await(5,java.util.concurrent.TimeUnit.SECONDS));worker.poll();verify(service,times(1)).pollVacancies();release.countDown();assertTrue(finished.await(5,java.util.concurrent.TimeUnit.SECONDS));}finally{release.countDown();worker.close();}}
}
