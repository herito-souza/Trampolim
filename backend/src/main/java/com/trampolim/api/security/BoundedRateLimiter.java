package com.trampolim.api.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Bounded per-instance limiter. Saturation rejects new keys instead of evicting active limits. */
public final class BoundedRateLimiter {
    private record Window(Instant expires, int count) {}
    private final Map<String,Window> windows=new HashMap<>();
    private final int capacity;private final Clock clock;private Instant sweep=Instant.MIN;
    public BoundedRateLimiter(){this(10000,Clock.systemUTC());}
    public BoundedRateLimiter(int capacity,Clock clock){if(capacity<1)throw new IllegalArgumentException();this.capacity=capacity;this.clock=clock;}
    public synchronized boolean allow(String key,int maximum,Duration duration) {
        if(key==null||key.length()>1024||maximum<1||duration.isNegative()||duration.isZero())return false;
        Instant now=clock.instant();
        if(!now.isBefore(sweep)||windows.size()>=capacity&&!windows.containsKey(key)) {
            windows.entrySet().removeIf(entry->!entry.getValue().expires().isAfter(now));sweep=now.plusSeconds(30);
        }
        Window old=windows.get(key);
        if(old==null||!old.expires().isAfter(now)) {
            if(old==null&&windows.size()>=capacity)return false;
            windows.put(key,new Window(now.plus(duration),1));return true;
        }
        if(old.count()>=maximum)return false;
        windows.put(key,new Window(old.expires(),old.count()+1));return true;
    }
    public synchronized int size(){return windows.size();}
}
