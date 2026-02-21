package com.nexaedi.core.model;

/**
 * Lifecycle states of an EDI file as it moves through the NexaEDI pipeline.
 */
public enum EdiProcessingStatus {

    /**
     * File has arrived and been persisted to S3 / local DLQ directory.
     */
    RECEIVED,

    /**
     * Raw X12 content has been successfully parsed into the Canonical Data Model.
     */
    PARSED,

    /**
     * The canonical order has been validated and is queued for outbound transmission.
     */
    VALIDATED,

    /**
     * The canonical order has been successfully sent to the downstream system (e.g. Shopify).
     */
    TRANSMITTED,

    /**
     * The downstream system has confirmed receipt / acceptance (e.g., 997 Functional Acknowledgment).
     */
    ACKNOWLEDGED,

    /**
     * Processing failed at some stage; a .error file exists in the Dead Letter Queue.
     */
    FAILED
}
