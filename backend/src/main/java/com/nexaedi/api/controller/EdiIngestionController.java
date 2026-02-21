package com.nexaedi.api.controller;

import com.nexaedi.api.dto.EdiSubmissionRequest;
import com.nexaedi.api.dto.ProcessingResponse;
import com.nexaedi.auth.service.JwtService;
import com.nexaedi.core.service.EdiOrchestrationService;
import com.nexaedi.infrastructure.persistence.EdiAuditLog;
import com.nexaedi.infrastructure.persistence.EdiAuditLogRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST API for submitting EDI files to the NexaEDI pipeline.
 * Supports two ingestion modes:
 *  1. JSON body — for programmatic integrations and testing
 *  2. Multipart file upload — for direct file delivery (e.g., from SFTP gateway scripts)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/edi")
@RequiredArgsConstructor
public class EdiIngestionController {

    private final EdiOrchestrationService orchestrationService;
    private final EdiAuditLogRepository auditLogRepository;
    private final JwtService jwtService;

    /**
     * Submits an EDI file for async processing via JSON    body.
     * Returns immediately with a correlationId for tracking.
     */
    @PostMapping("/ingest")
    public ResponseEntity<ProcessingResponse> ingest(
            @Valid @RequestBody EdiSubmissionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("[API] EDI ingest request — retailer={} file={}", request.getRetailerId(), request.getFileName());

        Long sellerId = extractSellerId(authHeader);
        String correlationId = UUID.randomUUID().toString();
        orchestrationService.processAsync(correlationId, request.getRetailerId(), request.getEdiContent(), request.getFileName(), sellerId);

        return ResponseEntity.accepted().body(buildResponse(correlationId, request.getRetailerId()));
    }

    /**
     * Submits an EDI file for async processing via multipart file upload.
     * Useful for direct SFTP → HTTP gateway integrations.
     */
    @PostMapping("/ingest/upload")
    public ResponseEntity<ProcessingResponse> ingestFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("retailerId") String retailerId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws IOException {

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.edi";

        log.info("[API] EDI file upload — retailer={} file={} size={} bytes", retailerId, fileName, file.getSize());

        Long sellerId = extractSellerId(authHeader);
        String correlationId = UUID.randomUUID().toString();
        orchestrationService.processAsync(correlationId, retailerId, content, fileName, sellerId);

        return ResponseEntity.accepted().body(buildResponse(correlationId, retailerId));
    }

    private Long extractSellerId(String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return jwtService.extractSellerId(authHeader.substring(7));
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Retrieves the full audit trail for a specific processing run.
     * Clients poll this endpoint to track the lifecycle of a submitted file.
     */
    @GetMapping("/audit/{correlationId}")
    public ResponseEntity<List<EdiAuditLog>> getAuditTrail(@PathVariable String correlationId) {
        List<EdiAuditLog> logs = auditLogRepository.findByCorrelationIdOrderByCreatedAtAsc(correlationId);
        if (logs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(logs);
    }

    /**
     * Health summary: counts of files by status. Useful for operational dashboards.
     */
    @GetMapping("/status/summary")
    public ResponseEntity<Object> getStatusSummary() {
        return ResponseEntity.ok(java.util.Map.of(
                "received",     auditLogRepository.countByStatus(com.nexaedi.core.model.EdiProcessingStatus.RECEIVED),
                "parsed",       auditLogRepository.countByStatus(com.nexaedi.core.model.EdiProcessingStatus.PARSED),
                "transmitted",  auditLogRepository.countByStatus(com.nexaedi.core.model.EdiProcessingStatus.TRANSMITTED),
                "acknowledged", auditLogRepository.countByStatus(com.nexaedi.core.model.EdiProcessingStatus.ACKNOWLEDGED),
                "failed",       auditLogRepository.countByStatus(com.nexaedi.core.model.EdiProcessingStatus.FAILED)
        ));
    }

    private ProcessingResponse buildResponse(String correlationId, String retailerId) {
        return ProcessingResponse.builder()
                .correlationId(correlationId)
                .message(String.format(
                        "EDI file accepted for async processing. Retailer: %s. Poll the audit trail for status updates.",
                        retailerId))
                .acceptedAt(Instant.now())
                .auditTrailUrl("/api/v1/edi/audit/" + correlationId)
                .build();
    }
}
