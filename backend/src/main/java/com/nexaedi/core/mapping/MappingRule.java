package com.nexaedi.core.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * A single field mapping rule defined within a MappingProfile JSON file.
 *
 * Example JSON entry:
 * {
 *   "segmentId": "BEG",
 *   "elementPosition": 3,
 *   "targetField": "poNumber",
 *   "required": true,
 *   "defaultValue": null
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MappingRule {

    /**
     * The X12 segment identifier (e.g., "BEG", "PO1", "N1").
     */
    private String segmentId;

    /**
     * 1-based element position within the segment (e.g., 3 = BEG03).
     */
    private int elementPosition;

    /**
     * The target field name on the CanonicalOrder or CanonicalOrderLine.
     */
    private String targetField;

    /**
     * Whether parsing should fail if this element is missing or empty.
     */
    private boolean required;

    /**
     * Optional static default value to apply when the segment element is empty.
     */
    private String defaultValue;

    /**
     * Optional qualifier to filter segment occurrences (e.g., N101 = "ST" for ship-to).
     * Format: "elementPosition:expectedValue", e.g., "01:ST"
     */
    private String qualifier;

    /**
     * Denotes this rule applies to a repeating loop (e.g., PO1 line items).
     * When true, this rule maps to a field on CanonicalOrderLine instead of CanonicalOrder.
     */
    private boolean lineLevel;
}
