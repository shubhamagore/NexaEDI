package com.nexaedi.core.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * The Canonical Data Model (CDM) for a Purchase Order (850).
 * All retailer-specific EDI formats are normalized into this model before
 * any downstream transmission. This decouples parsers from adapters.
 */
@Data
@Builder
public class CanonicalOrder {

    /**
     * Unique identifier assigned by NexaEDI for internal tracking.
     */
    @NotBlank(message = "Correlation ID must not be blank")
    private String correlationId;

    /**
     * The retailer who sent this purchase order (e.g., "TARGET", "WALMART").
     */
    @NotBlank(message = "Retailer ID must not be blank")
    private String retailerId;

    /**
     * The retailer's own PO number (from BEG03).
     */
    @NotBlank(message = "PO number must not be blank")
    private String poNumber;

    /**
     * The type of PO (e.g., SA = Stand-Alone, OS = Drop-Ship).
     */
    @NotBlank
    private String purchaseOrderType;

    /**
     * Date the purchase order was issued by the retailer.
     */
    @NotNull(message = "PO date must not be null")
    private LocalDate poDate;

    /**
     * The date by which goods must be delivered to the retailer's DC.
     */
    private LocalDate requestedDeliveryDate;

    /**
     * Ship-to address details parsed from the N1/N3/N4 loop.
     */
    @NotBlank(message = "Ship-to name must not be blank")
    private String shipToName;

    private String shipToAddress;
    private String shipToCity;
    private String shipToState;
    private String shipToZip;

    /**
     * Department number assigned by the retailer (from BEG09 or REF).
     */
    private String departmentNumber;

    /**
     * Individual line items on the purchase order.
     */
    @NotEmpty(message = "A Purchase Order must have at least one line item")
    @Valid
    private List<CanonicalOrderLine> lines;

    /**
     * Raw X12 interchange control number for traceability.
     */
    private String interchangeControlNumber;

    /**
     * Raw X12 transaction set control number for traceability.
     */
    private String transactionControlNumber;
}
