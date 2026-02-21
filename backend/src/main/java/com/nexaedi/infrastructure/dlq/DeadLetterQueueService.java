package com.nexaedi.infrastructure.dlq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Manages the Dead Letter Queue (DLQ) for EDI files that fail processing.
 *
 * On failure, this service:
 *  1. Copies the original EDI file to the DLQ directory, preserving the file name.
 *  2. Creates a companion .error file with a timestamped failure report including
 *     the exact segment, line number, and full exception message.
 *
 * The DLQ directory is monitored for operational alerting. Files can be corrected
 * and resubmitted through the ingestion API for replay.
 */
@Slf4j
@Service
public class DeadLetterQueueService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);

    @Value("${nexaedi.dlq.directory:dlq}")
    private String dlqDirectory;

    /**
     * Moves a failed EDI file to the DLQ and writes a companion error report.
     *
     * @param correlationId  the unique ID for this processing run
     * @param retailerId     the retailer that sent the file
     * @param originalContent the raw EDI content that failed
     * @param originalFileName the original filename for reference
     * @param errorMessage   the human-readable error description
     * @param cause          the exception that triggered the failure
     * @return the Path to the error file for logging
     */
    public Path quarantine(String correlationId, String retailerId, String originalContent,
                           String originalFileName, String errorMessage, Throwable cause) {
        try {
            Path dlqPath = Path.of(dlqDirectory, retailerId.toLowerCase());
            Files.createDirectories(dlqPath);

            String timestamp = TIMESTAMP_FORMAT.format(Instant.now());
            String baseName = correlationId + "_" + timestamp;

            Path ediFile = dlqPath.resolve(baseName + ".edi");
            Files.writeString(ediFile, originalContent != null ? originalContent : "", StandardCharsets.UTF_8);

            Path errorFile = dlqPath.resolve(baseName + ".error");
            String errorReport = buildErrorReport(correlationId, retailerId, originalFileName, errorMessage, cause);
            Files.writeString(errorFile, errorReport, StandardCharsets.UTF_8);

            log.warn("[DLQ] Quarantined failed EDI file. correlationId={} retailer={} errorFile={}",
                    correlationId, retailerId, errorFile.toAbsolutePath());

            return errorFile;
        } catch (IOException e) {
            log.error("[DLQ] CRITICAL: Failed to write to Dead Letter Queue for correlationId={}: {}",
                    correlationId, e.getMessage(), e);
            return null;
        }
    }

    private String buildErrorReport(String correlationId, String retailerId, String originalFileName,
                                    String errorMessage, Throwable cause) {
        StringBuilder report = new StringBuilder();
        report.append("=== NexaEDI Dead Letter Queue Error Report ===\n");
        report.append("Timestamp     : ").append(Instant.now()).append("\n");
        report.append("Correlation ID: ").append(correlationId).append("\n");
        report.append("Retailer      : ").append(retailerId).append("\n");
        report.append("Original File : ").append(originalFileName).append("\n");
        report.append("\n--- Error ---\n");
        report.append(errorMessage).append("\n");

        if (cause != null) {
            report.append("\n--- Exception ---\n");
            report.append(cause.getClass().getName()).append(": ").append(cause.getMessage()).append("\n");
            for (StackTraceElement element : cause.getStackTrace()) {
                report.append("  at ").append(element.toString()).append("\n");
                if (element.getClassName().startsWith("com.nexaedi")) {
                    break;
                }
            }
        }

        report.append("\n--- Resolution Steps ---\n");
        report.append("1. Correct the EDI segment/element identified in the error above.\n");
        report.append("2. Resubmit the corrected file to POST /api/v1/edi/ingest\n");
        report.append("   with the header X-Correlation-Id: ").append(correlationId).append("\n");
        report.append("   for traceability continuity.\n");
        report.append("==============================================\n");

        return report.toString();
    }
}
