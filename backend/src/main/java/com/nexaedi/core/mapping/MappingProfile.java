package com.nexaedi.core.mapping;

import lombok.Data;

import java.util.List;

/**
 * The complete mapping configuration for a specific Retailer + Transaction Type combination.
 * Loaded from a JSON file in the /mappings directory at startup.
 *
 * File naming convention: {retailer-id}-{transaction-set}.json
 * Example: target-850.json, walmart-850.json, amazon-856.json
 */
@Data
public class MappingProfile {

    /**
     * Canonical retailer identifier (must be UPPER_CASE, no spaces).
     * Example: "TARGET", "WALMART", "AMAZON"
     */
    private String retailerId;

    /**
     * X12 transaction set number this profile handles.
     * Example: "850" (Purchase Order), "856" (Ship Notice), "810" (Invoice)
     */
    private String transactionSetCode;

    /**
     * Human-readable description for auditing and documentation.
     */
    private String description;

    /**
     * Version of this mapping profile for change management.
     */
    private String version;

    /**
     * The element delimiter character used by this retailer.
     * Default per X12 standard is '*'. Some retailers may use a different character.
     */
    private char elementDelimiter = '*';

    /**
     * All field-level mapping rules for header-level CDM fields.
     */
    private List<MappingRule> headerMappings;

    /**
     * All field-level mapping rules for line-item (PO1 loop) CDM fields.
     */
    private List<MappingRule> lineMappings;
}
