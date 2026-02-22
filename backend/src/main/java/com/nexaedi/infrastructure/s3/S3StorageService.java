package com.nexaedi.infrastructure.s3;

import com.nexaedi.infrastructure.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import com.nexaedi.infrastructure.storage.StorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;


@Slf4j
@Service
@ConditionalOnProperty(
        name = "nexaedi.s3.enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private static final DateTimeFormatter DATE_PREFIX_FORMAT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    @Override
    public String storeInbound(String correlationId, String retailerId, String content) {

        String datePrefix = DATE_PREFIX_FORMAT.format(Instant.now());
        String key = String.format("%s%s/%s/%s.edi",
                s3Properties.getInboundPrefix(),
                datePrefix,
                retailerId.toLowerCase(),
                correlationId);

        putObject(key, content, retailerId);
        return key;
    }

    @Override
    public String storeOutbound(String correlationId, String retailerId, String content) {

        String key = String.format("%s/%s-outbound.edi",
                s3Properties.getOutboundPrefix(),
                correlationId);

        putObject(key, content, retailerId);
        return key;
    }

    @Override
    public String retrieveContent(String key) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .build();

        byte[] bytes = s3Client.getObjectAsBytes(request).asByteArray();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public String archiveProcessed(String key, String correlationId) {

        String archiveKey = key.replace(
                s3Properties.getInboundPrefix(),
                s3Properties.getProcessedPrefix());

        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(s3Properties.getBucketName())
                .sourceKey(key)
                .destinationBucket(s3Properties.getBucketName())
                .destinationKey(archiveKey)
                .build();

        s3Client.copyObject(copyRequest);
        return archiveKey;
    }

    private void putObject(String key, String content, String retailerId) {

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .contentType("application/edi-x12")
                .contentLength((long) bytes.length)
                .tagging("retailer=" + retailerId)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));
    }
    
}
