package com.nexaedi.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized AWS S3 configuration for EDI file storage.
 * Bound from the "nexaedi.s3" prefix in application.yml.
 */
@Data
@ConfigurationProperties(prefix = "nexaedi.s3")
public class S3Properties {

    /**
     * AWS region where the S3 bucket resides (e.g., "us-east-1").
     */
    private String region = "us-east-1";

    /**
     * The S3 bucket name for storing raw EDI files.
     */
    private String bucketName;

    /**
     * Prefix/folder within the bucket for inbound EDI files.
     */
    private String inboundPrefix = "edi/inbound/";

    /**
     * Prefix/folder for successfully processed EDI files (archival).
     */
    private String processedPrefix = "edi/processed/";
}
