package com.rce.execution_engine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RateLimiterService {

    // How often the cleanup sweep runs - a fixed operational cadence, independent of the
    // per-instance window/limit policy below (which is why it stays a compile-time constant).
    private static final long EVICTION_SWEEP_MILLIS = 10 * 60_000;

    private record Bucket(AtomicInteger count, AtomicLong windowStart) {}

    private final int maxRequestsPerWindow;
    private final long windowMillis;
    private final long staleAfterMillis;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${rce.rate-limit.max-requests:5}") int maxRequestsPerWindow,
            @Value("${rce.rate-limit.window-millis:60000}") long windowMillis) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowMillis = windowMillis;
        this.staleAfterMillis = windowMillis * 10;
    }

    // Returns true if this client is still within its quota for the current window.
    public boolean tryAcquire(String clientId) {
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.computeIfAbsent(clientId, id -> new Bucket(new AtomicInteger(0), new AtomicLong(now)));

        long windowStart = bucket.windowStart().get();
        if (now - windowStart >= windowMillis) {
            // Window expired - whichever thread wins the CAS resets it for everyone else.
            if (bucket.windowStart().compareAndSet(windowStart, now)) {
                bucket.count().set(0);
            }
        }

        return bucket.count().incrementAndGet() <= maxRequestsPerWindow;
    }

    // Without this, every distinct client IP that ever hits the API stays in the map forever.
    @Scheduled(fixedRate = EVICTION_SWEEP_MILLIS)
    void evictStaleEntries() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> now - entry.getValue().windowStart().get() > staleAfterMillis);
    }
}
