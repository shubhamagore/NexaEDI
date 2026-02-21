package com.nexaedi.infrastructure.config.local;

import com.nexaedi.infrastructure.config.S3Properties;
import com.nexaedi.infrastructure.s3.S3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Local development configuration.
 * Active only when the "local" Spring profile is used.
 *
 * Replaces three external dependencies with zero-infrastructure stubs:
 *   1. S3Client        → No-op stub (prevents AWS SDK from trying to connect)
 *   2. S3StorageService→ Writes files to ./local-storage/ instead of AWS S3
 *   3. ShopifyOutboundAdapter → Returns a fake draft order ID, logs the payload
 */
@Slf4j
@Configuration
@Profile("local")
public class LocalDevConfig {

    private static final DateTimeFormatter DATE_PREFIX =
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    @Value("${nexaedi.local.storage-dir:local-storage}")
    private String localStorageDir;

    /**
     * No-op S3Client stub — prevents AWS SDK from attempting to contact AWS.
     */
    @Bean
    @Primary
    public S3Client s3Client() {
        log.info("[LOCAL] S3Client stub active — files will be stored in '{}'", localStorageDir);
        return S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .credentialsProvider(
                        software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                                software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("local", "local")
                        )
                )
                .endpointOverride(java.net.URI.create("http://localhost:9999"))
                .build();
    }

    /**
     * Local filesystem implementation of S3StorageService.
     * Saves EDI files to ./local-storage/inbound/{date}/{retailer}/{correlationId}.edi
     */
    @Bean
    @Primary
    public S3StorageService s3StorageService(S3Properties s3Properties) {
        return new S3StorageService(s3Client(), s3Properties) {

            @Override
            public String storeInbound(String correlationId, String retailerId, String ediContent) {
                String datePrefix = DATE_PREFIX.format(Instant.now());
                String relativePath = String.format("inbound/%s/%s/%s.edi",
                        datePrefix, retailerId.toLowerCase(), correlationId);
                Path filePath = Path.of(localStorageDir, relativePath);
                writeLocalFile(filePath, ediContent);
                log.info("[LOCAL-S3] Stored inbound EDI: {}", filePath.toAbsolutePath());
                return "local://" + relativePath;
            }

            @Override
            public String archiveProcessed(String inboundS3Key, String correlationId) {
                String archivePath = inboundS3Key.replace("inbound/", "processed/");
                log.info("[LOCAL-S3] Archived processed file: {}", archivePath);
                return archivePath;
            }

            @Override
            public String retrieveContent(String s3Key) {
                Path filePath = Path.of(localStorageDir, s3Key.replace("local://", ""));
                try {
                    return Files.readString(filePath, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    log.warn("[LOCAL-S3] Could not read file '{}': {}", filePath, e.getMessage());
                    return "";
                }
            }

            private void writeLocalFile(Path filePath, String content) {
                try {
                    Files.createDirectories(filePath.getParent());
                    Files.writeString(filePath, content, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    log.error("[LOCAL-S3] Failed to write local file '{}': {}", filePath, e.getMessage());
                }
            }
        };
    }

}
