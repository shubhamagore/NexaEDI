package com.nexaedi.core.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * A single line item within a Canonical Order, representing one product/SKU being ordered.
 */
@Data
@Builder
public class CanonicalOrderLine {

    /**
     * Sequential line number within the PO (from PO1 loop).
     */
    @NotNull
    @Positive
    private Integer lineSequenceNumber;

    /**
     * Retailer-assigned SKU / UPC / GTIN identifier.
     */
    @NotBlank(message = "SKU must not be blank on order line")
    private String sku;

    /**
     * Quantity ordered.
     */
    @NotNull
    @Min(value = 1, message = "Ordered quantity must be at least 1")
    private Integer quantityOrdered;

    /**
     * Unit of measure (e.g., EA = Each, CA = Case, DZ = Dozen).
     */
    @NotBlank
    private String unitOfMeasure;

    /**
     * Agreed unit price from the EDI transaction.
     */
    @NotNull
    @Positive
    private BigDecimal unitPrice;

    /**
     * Vendor-assigned product description for human readability.
     */
    private String productDescription;
}
