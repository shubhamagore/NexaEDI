package com.nexaedi.infrastructure.dlq;

import com.nexaedi.infrastructure.persistence.DeadLetterEntry;
import com.nexaedi.infrastructure.persistence.DeadLetterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final DeadLetterRepository deadLetterRepository;

    public DeadLetterEntry quarantine(String correlationId, String retailerId, String originalContent,
                                      String originalFileName, String errorMessage, Throwable cause) {
        try {
            String errorReport = buildErrorReport(correlationId, retailerId, originalFileName, errorMessage, cause);
            DeadLetterEntry entry = DeadLetterEntry.builder()
                    .id(UUID.randomUUID())
                    .retailerId(retailerId != null ? retailerId.toLowerCase() : "")
                    .correlationId(correlationId)
                    .originalContent(originalContent != null ? originalContent : "")
                    .errorReport(errorReport)
                    .createdAt(Instant.now())
                    .build();
            entry = deadLetterRepository.save(entry);
            log.warn("[DLQ] Quarantined failed EDI file. correlationId={} retailer={} id={}",
                    correlationId, retailerId, entry.getId());
            return entry;
        } catch (Exception e) {
            log.error("[DLQ] CRITICAL: Failed to persist Dead Letter Queue entry for correlationId={}: {}",
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