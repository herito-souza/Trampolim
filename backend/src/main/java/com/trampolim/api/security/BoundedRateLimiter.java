package com.trampolim.api.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Comparator;

/** Bounded per-instance limiter. Saturation rejects new keys instead of evicting active limits. */
public final class BoundedRateLimiter {
    private record Window(Instant expires, int count) {}
    private final Map<String,Window> windows=new HashMap<>();
    private record Expiry(String key,Instant at) {}
    private final PriorityQueue<Expiry> expiry=new PriorityQueue<>(Comparator.comparing(Expiry::at));
    private final int capacity;private final Clock clock;
    public BoundedRateLimiter(){this(10000,Clock.systemUTC());}
    public BoundedRateLimiter(int capacity,Clock clock){if(capacity<1)throw new IllegalArgumentException();this.capacity=capacity;this.clock=clock;}
    public synchronized boolean allow(String key,int maximum,Duration duration) {
        if(key==null||key.length()>1024||maximum<1||duration.isNegative()||duration.isZero())return false;
        Instant now=clock.instant();
        // Each active key contributes one expiry entry. Never scan all keys under saturation.
        while(!expiry.isEmpty()&&!expiry.peek().at().isAfter(now)) {
            var expired=expiry.remove();var window=windows.get(expired.key());
            if(window!=null&&window.expires().equals(expired.at()))windows.remove(expired.key());
        }
        Window old=windows.get(key);
        if(old==null||!old.expires().isAfter(now)) {
            if(old==null&&windows.size()>=capacity)return false;
            Instant expires=now.plus(duration);windows.put(key,new Window(expires,1));expiry.add(new Expiry(key,expires));return true;
        }
        if(old.count()>=maximum)return false;
        windows.put(key,new Window(old.expires(),old.count()+1));return true;
    }
    public synchronized int size(){return windows.size();}
}
