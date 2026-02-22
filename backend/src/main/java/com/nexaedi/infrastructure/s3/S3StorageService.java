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


/**
 * Service for storing and retrieving EDI files from AWS S3.
 * All inbound files are written to S3 immediately upon receipt,
 * providing a durable, replayable audit trail independent of the database.
 */
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

    /**
     * Stores a raw EDI file in the inbound S3 prefix.
     *
     * @param correlationId  unique ID for this processing run (used as part of the S3 key)
     * @param retailerId     originating retailer (used as a folder partition)
     * @param ediContent     the raw X12 content
     * @return the full S3 key where the file was stored
     */
    public String storeInbound(String correlationId, String retailerId, String ediContent) {
        String datePrefix = DATE_PREFIX_FORMAT.format(Instant.now());
        String s3Key = String.format("%s%s/%s/%s.edi",
                s3Properties.getInboundPrefix(), datePrefix, retailerId.toLowerCase(), correlationId);

        putObject(s3Key, ediContent, retailerId);
        log.info("[S3] Stored inbound EDI file: s3://{}/{}", s3Properties.getBucketName(), s3Key);
        return s3Key;
    }

    /**
     * Archives a successfully processed EDI file by copying it to the processed prefix
     * and returning the new archive key.
     */
    public String archiveProcessed(String inboundS3Key, String correlationId) {
        String datePrefix = DATE_PREFIX_FORMAT.format(Instant.now());
        String archiveKey = inboundS3Key
                .replace(s3Properties.getInboundPrefix(), s3Properties.getProcessedPrefix());

        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(s3Properties.getBucketName())
                .sourceKey(inboundS3Key)
                .destinationBucket(s3Properties.getBucketName())
                .destinationKey(archiveKey)
                .build();

        s3Client.copyObject(copyRequest);
        log.info("[S3] Archived processed file: s3://{}/{}", s3Properties.getBucketName(), archiveKey);
        return archiveKey;
    }

     @Override
    public String storeOutbound(String correlationId, String retailerId, String content) {
        return objectKey;
    }
     @Override
    public String retrieveContent(String key) {
        return content;
    }

    @Override
    public String archiveProcessed(String key, String correlationId) {
        return archivedKey;
    }

    /**
     * Retrieves the raw EDI content of a file by its S3 key (for replay/reprocessing).
     */
    public String retrieveContent(String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(s3Key)
                .build();

        byte[] bytes = s3Client.getObjectAsBytes(request).asByteArray();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void putObject(String s3Key, String content, String retailerId) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(s3Key)
                .contentType("application/edi-x12")
                .contentLength((long) bytes.length)
                .tagging("retailer=" + retailerId + "&platform=nexaedi")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));
    }
}
