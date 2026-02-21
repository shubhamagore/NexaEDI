package com.nexaedi.infrastructure.persistence;

import com.nexaedi.core.model.EdiProcessingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Immutable audit record for every EDI file processed by NexaEDI.
 * Each status transition creates a new row, preserving a full lifecycle history.
 */
@Entity
@Table(
    name = "edi_audit_log",
    indexes = {
        @Index(name = "idx_audit_correlation_id", columnList = "correlation_id"),
        @Index(name = "idx_audit_retailer_status", columnList = "retailer_id, status"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdiAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier for this EDI processing run; correlates all audit rows for one file.
     */
    @Column(name = "correlation_id", nullable = false, length = 36)
    private String correlationId;

    /**
     * Originating retailer (e.g., "TARGET", "WALMART").
     */
    @Column(name = "retailer_id", nullable = false, length = 50)
    private String retailerId;

    /**
     * X12 transaction set code (e.g., "850", "856").
     */
    @Column(name = "transaction_set_code", length = 10)
    private String transactionSetCode;

    /**
     * The retailer's PO number extracted from the transaction, if available.
     */
    @Column(name = "po_number", length = 50)
    private String poNumber;

    /**
     * The lifecycle status at the time this audit row was created.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EdiProcessingStatus status;

    /**
     * Path or S3 key of the original EDI file for reference and replay.
     */
    @Column(name = "source_file_path", length = 1024)
    private String sourceFilePath;

    /**
     * Human-readable summary of what happened at this stage.
     */
    @Column(name = "message", length = 2048)
    private String message;

    /**
     * Full error detail (stack trace excerpt) if the status is FAILED.
     */
    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    /**
     * Timestamp when this audit record was written.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Duration of the processing stage in milliseconds for performance monitoring.
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
