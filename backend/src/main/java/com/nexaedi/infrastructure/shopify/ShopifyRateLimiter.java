package com.nexaedi.infrastructure.shopify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implements Shopify's Leaky Bucket rate limiting algorithm.
 *
 * Shopify's Admin API enforces a bucket with:
 *  - A maximum capacity (e.g., 40 calls for standard, 80 for Plus)
 *  - A steady drain rate (e.g., 2 calls/second are released back)
 *
 * This implementation uses a Semaphore to model the bucket's available permits.
 * A background virtual thread refills permits at the configured drain rate,
 * ensuring NexaEDI never exceeds the allowed request burst.
 */
@Slf4j
@Component
public class ShopifyRateLimiter {

    private final Semaphore bucket;
    private final int capacity;
    private final ScheduledExecutorService refillScheduler;

    public ShopifyRateLimiter(ShopifyProperties properties) {
        this.capacity = properties.getBucketCapacity();
        this.bucket = new Semaphore(capacity, true);

        this.refillScheduler = Executors.newSingleThreadScheduledExecutor(
                Thread.ofVirtual().name("shopify-rate-limiter-refill", 0).factory()
        );

        int refillRate = properties.getRefillRatePerSecond();
        refillScheduler.scheduleAtFixedRate(
                this::refill,
                1, 1, TimeUnit.SECONDS
        );

        log.info("ShopifyRateLimiter initialized — capacity: {}, refill rate: {}/sec",
                capacity, refillRate);
    }

    /**
     * Acquires one permit from the leaky bucket, blocking if the bucket is full
     * (i.e., we are at rate limit). Uses a virtual thread to avoid platform thread starvation.
     *
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public void acquire() throws InterruptedException {
        log.debug("ShopifyRateLimiter — waiting for permit (available: {})", bucket.availablePermits());
        bucket.acquire();
        log.debug("ShopifyRateLimiter — permit acquired (remaining: {})", bucket.availablePermits());
    }

    /**
     * Refills the bucket by the configured drain rate each second.
     * Never exceeds the maximum capacity.
     */
    private void refill() {
        int permitsToRelease = Math.min(
                capacity - bucket.availablePermits(),
                capacity
        );
        if (permitsToRelease > 0) {
            bucket.release(permitsToRelease);
            log.trace("ShopifyRateLimiter — refilled {} permits (total: {})",
                    permitsToRelease, bucket.availablePermits());
        }
    }

    /**
     * Returns current available permits for observability/health checks.
     */
    public int availablePermits() {
        return bucket.availablePermits();
    }
}
