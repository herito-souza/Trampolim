package com.trampolim.api.security;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
class BoundedRateLimiterTest {
    @Test void concurrentAttemptsAreLimitedWithoutEvictionOrGrowth() throws Exception {
        var limiter=new BoundedRateLimiter(2,Clock.systemUTC());
        try(var executor=Executors.newFixedThreadPool(8)) {
            var requests=new java.util.ArrayList<Future<Boolean>>();for(int i=0;i<100;i++)requests.add(executor.submit(()->limiter.allow("same",12,Duration.ofMinutes(15))));
            int accepted=0;for(var result:requests)if(result.get())accepted++;assertEquals(12,accepted);
        }
        assertTrue(limiter.allow("second",1,Duration.ofHours(1)));assertFalse(limiter.allow("third",1,Duration.ofHours(1)));assertEquals(2,limiter.size());assertFalse(limiter.allow("same",12,Duration.ofMinutes(15)));
    }
    @Test void expiredWindowsReleaseCapacity() {
        class MutableClock extends Clock {Instant now=Instant.parse("2026-01-01T00:00:00Z");public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}}
        var clock=new MutableClock();var limiter=new BoundedRateLimiter(1,clock);assertTrue(limiter.allow("one",1,Duration.ofSeconds(5)));assertFalse(limiter.allow("one",1,Duration.ofSeconds(5)));clock.now=clock.now.plusSeconds(5);assertTrue(limiter.allow("two",1,Duration.ofSeconds(5)));assertEquals(1,limiter.size());
    }
    @Test void saturationDoesNotEvictLiveKeysAndExpiryHeapStaysBounded() {
        var limiter=new BoundedRateLimiter(10000,Clock.fixed(Instant.parse("2026-11-06T00:00:00Z"),ZoneOffset.UTC));
        for(int i=0;i<10000;i++)assertTrue(limiter.allow("live"+i,2,Duration.ofHours(1)));
        for(int i=0;i<100000;i++)assertFalse(limiter.allow("rejected"+i,2,Duration.ofHours(1)));
        assertEquals(10000,limiter.size());assertTrue(limiter.allow("live0",2,Duration.ofHours(1)));assertFalse(limiter.allow("live0",2,Duration.ofHours(1)));
        var queue=org.springframework.test.util.ReflectionTestUtils.getField(limiter,"expiry");assertEquals(10000,((java.util.Queue<?>)queue).size());
    }
}
