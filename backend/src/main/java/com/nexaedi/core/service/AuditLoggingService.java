package com.nexaedi.core.service;

import com.nexaedi.core.model.EdiProcessingStatus;
import com.nexaedi.infrastructure.persistence.EdiAuditLog;
import com.nexaedi.infrastructure.persistence.EdiAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service responsible for writing immutable audit trail records for every EDI file
 * lifecycle transition. Each call creates a new database row, forming an append-only log.
 *
 * Uses REQUIRES_NEW propagation so audit records are committed even if the
 * calling transaction rolls back — ensuring audit completeness on failure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLoggingService {

    private final EdiAuditLogRepository repository;

    /**
     * Records a lifecycle state transition for an EDI file.
     *
     * @param correlationId      unique identifier for this processing run
     * @param retailerId         originating retailer
     * @param transactionSetCode e.g. "850"
     * @param poNumber           the purchase order number, if already extracted
     * @param status             the new lifecycle status
     * @param sourceFilePath     S3 key or local path of the original file
     * @param message            human-readable summary of this transition
     * @param durationMs         how long this stage took in milliseconds
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EdiAuditLog record(String correlationId, String retailerId, String transactionSetCode,
                               String poNumber, EdiProcessingStatus status, String sourceFilePath,
                               String message, Long durationMs) {
        EdiAuditLog entry = EdiAuditLog.builder()
                .correlationId(correlationId)
                .retailerId(retailerId)
                .transactionSetCode(transactionSetCode)
                .poNumber(poNumber)
                .status(status)
                .sourceFilePath(sourceFilePath)
                .message(message)
                .durationMs(durationMs)
                .createdAt(Instant.now())
                .build();

        EdiAuditLog saved = repository.save(entry);
        log.info("[AUDIT] correlationId={} retailer={} poNumber={} status={} durationMs={}",
                correlationId, retailerId, poNumber, status, durationMs);
        return saved;
    }

    /**
     * Convenience overload for recording a failure with a full error detail payload.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EdiAuditLog recordFailure(String correlationId, String retailerId, String sourceFilePath,
                                      String message, String errorDetail) {
        EdiAuditLog entry = EdiAuditLog.builder()
                .correlationId(correlationId)
                .retailerId(retailerId)
                .status(EdiProcessingStatus.FAILED)
                .sourceFilePath(sourceFilePath)
                .message(message)
                .errorDetail(errorDetail)
                .createdAt(Instant.now())
                .build();

        EdiAuditLog saved = repository.save(entry);
        log.error("[AUDIT-FAILURE] correlationId={} retailer={} message={}", correlationId, retailerId, message);
        return saved;
    }
}
