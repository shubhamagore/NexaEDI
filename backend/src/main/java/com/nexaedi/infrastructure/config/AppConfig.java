package com.nexaedi.infrastructure.config;

import com.nexaedi.infrastructure.shopify.ShopifyProperties;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Central application configuration.
 *
 * - Virtual Threads: All async EDI processing runs on Project Loom virtual threads.
 * - Spring Retry: @EnableRetry activates @Retryable on ShopifyOutboundAdapter.
 * - ShopifyProperties is NOT injected here — it is wired directly into
 *   ShopifyOutboundAdapter by Spring's component scan, avoiding circular dependencies.
 */
@Configuration
@EnableRetry
@EnableAsync
@EnableConfigurationProperties({ShopifyProperties.class, S3Properties.class})
public class AppConfig {

    /**
     * Virtual thread executor for high-concurrency EDI batch processing.
     */
    @Bean(name = "ediVirtualThreadExecutor")
    public Executor ediVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * ObjectMapper for parsing mapping profile JSON files.
     * Spring Boot 4.x uses Jackson 3.x (tools.jackson.*).
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Pre-configured RestClient for Shopify Admin API calls.
     */
    @Bean
    public RestClient shopifyRestClient() {
        return RestClient.builder()
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * AWS S3 client — skipped when the local profile defines its own stub via @Primary.
     */
    @Bean
    @ConditionalOnMissingBean
    public S3Client s3Client(S3Properties s3Properties) {
        return S3Client.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
