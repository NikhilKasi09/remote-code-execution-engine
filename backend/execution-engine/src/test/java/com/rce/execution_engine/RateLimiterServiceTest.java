package com.rce.execution_engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    @Test
    void allowsRequestsUpToTheLimitThenBlocks() {
        RateLimiterService limiter = new RateLimiterService(3, 60_000);

        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
        assertThat(limiter.tryAcquire("1.2.3.4")).isFalse();
    }

    @Test
    void tracksEachClientIndependently() {
        RateLimiterService limiter = new RateLimiterService(1, 60_000);

        assertThat(limiter.tryAcquire("client-a")).isTrue();
        assertThat(limiter.tryAcquire("client-a")).isFalse();

        // A different client has its own quota, unaffected by client-a's usage.
        assertThat(limiter.tryAcquire("client-b")).isTrue();
    }

    @Test
    void resetsQuotaOnceTheWindowExpires() throws InterruptedException {
        RateLimiterService limiter = new RateLimiterService(1, 100);

        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
        assertThat(limiter.tryAcquire("1.2.3.4")).isFalse();

        Thread.sleep(150);

        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
    }

    @Test
    void neverAllowsMoreThanTheLimitUnderConcurrentAccess() throws Exception {
        int limit = 20;
        int attempts = 200;
        RateLimiterService limiter = new RateLimiterService(limit, 60_000);
        ExecutorService pool = Executors.newFixedThreadPool(16);

        try {
            List<Callable<Boolean>> tasks = java.util.stream.IntStream.range(0, attempts)
                    .<Callable<Boolean>>mapToObj(i -> () -> limiter.tryAcquire("shared-client"))
                    .toList();

            List<Future<Boolean>> results = pool.invokeAll(tasks);

            AtomicInteger allowed = new AtomicInteger();
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    allowed.incrementAndGet();
                }
            }

            // The whole point of the CAS-based bucket is that concurrent callers can't
            // race their way past the limit - exactly `limit` should get through, no more.
            assertThat(allowed.get()).isEqualTo(limit);
        } finally {
            pool.shutdown();
        }
    }
}
