package com.nexaedi.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * HTTP request DTO for submitting a raw EDI file to the NexaEDI ingestion endpoint.
 */
@Data
public class EdiSubmissionRequest {

    /**
     * The retailer identifier. Must match a loaded MappingProfile (e.g., "TARGET", "WALMART").
     */
    @NotBlank(message = "retailerId is required")
    private String retailerId;

    /**
     * The raw X12 EDI content as a plain-text string.
     */
    @NotBlank(message = "ediContent is required")
    private String ediContent;

    /**
     * Original filename for audit trail and DLQ error reporting.
     */
    private String fileName = "unknown.edi";
}
