package com.nexaedi.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * HTTP response DTO returned immediately after EDI file ingestion.
 * Since processing is asynchronous, this confirms receipt, not completion.
 */
@Data
@Builder
public class ProcessingResponse {

    /**
     * The unique correlation ID assigned to this processing run.
     * Use this ID to query the audit log for lifecycle status updates.
     */
    private String correlationId;

    /**
     * Confirmation message.
     */
    private String message;

    /**
     * Timestamp when the file was accepted by NexaEDI.
     */
    private Instant acceptedAt;

    /**
     * HTTP path to query this file's audit trail.
     */
    private String auditTrailUrl;
}
