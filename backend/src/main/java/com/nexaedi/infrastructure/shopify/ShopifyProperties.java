package com.nexaedi.infrastructure.shopify;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the Shopify 2026 Admin API integration.
 * Bound from the "nexaedi.shopify" prefix in application.yml.
 * Registered as a bean via @EnableConfigurationProperties in AppConfig — not @Component.
 */
@Data
@ConfigurationProperties(prefix = "nexaedi.shopify")
public class ShopifyProperties {

    /**
     * Shopify store subdomain, e.g., "my-store" for my-store.myshopify.com
     */
    private String storeName;

    /**
     * Client ID from the Shopify Partner App (Client Credentials OAuth2 flow).
     */
    private String clientId;

    /**
     * Client secret from the Shopify Partner App.
     */
    private String clientSecret;

    /**
     * Shopify API version to use. Default: 2026-01
     */
    private String apiVersion = "2026-01";

    /**
     * Leaky bucket capacity: maximum API calls allowed in the bucket.
     * Shopify standard is 40 calls/bucket, Plus is 80.
     */
    private int bucketCapacity = 40;

    /**
     * Bucket refill rate: calls restored per second by Shopify's algorithm.
     */
    private int refillRatePerSecond = 2;

    /**
     * Maximum number of retry attempts for failed API calls.
     */
    private int maxRetryAttempts = 3;

    /**
     * Base backoff delay in milliseconds between retries.
     */
    private long retryBackoffMs = 1000;
}
