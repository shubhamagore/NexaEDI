package com.nexaedi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * NexaEDI — High-performance, open-source EDI Orchestration Platform.
 *
 * Entry point for the Spring Boot application. Virtual Threads are activated
 * via application.yml (spring.threads.virtual.enabled=true), enabling
 * thousands of concurrent EDI file processings per JVM instance.
 *
 * Key component scan packages:
 *  - com.nexaedi.core         : Parser, Mapper, Processor, Services
 *  - com.nexaedi.api          : REST Controllers and DTOs
 *  - com.nexaedi.infrastructure: JPA, S3, Shopify, DLQ, Config
 */
@SpringBootApplication
@EnableAsync
public class NexaediApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexaediApplication.class, args);
    }
}
