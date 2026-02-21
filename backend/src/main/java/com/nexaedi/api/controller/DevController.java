package com.nexaedi.api.controller;

import com.nexaedi.infrastructure.persistence.EdiAuditLog;
import com.nexaedi.infrastructure.persistence.EdiAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dev-only endpoints for inspecting in-memory H2 data without a DB console.
 * Only active when the "local" Spring profile is running.
 * Never included in production builds.
 */
@RestController
@RequestMapping("/dev")
@Profile("local")
@RequiredArgsConstructor
public class DevController {

    private final EdiAuditLogRepository auditLogRepository;

    /**
     * Returns all audit log records — equivalent to: SELECT * FROM EDI_AUDIT_LOG
     */
    @GetMapping("/audit-log")
    public ResponseEntity<List<EdiAuditLog>> getAllAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    /**
     * Returns audit records for a specific correlation ID.
     */
    @GetMapping("/audit-log/{correlationId}")
    public ResponseEntity<List<EdiAuditLog>> getByCorrelationId(@PathVariable String correlationId) {
        return ResponseEntity.ok(
                auditLogRepository.findByCorrelationIdOrderByCreatedAtAsc(correlationId));
    }

    /**
     * Returns a live count of all records grouped by status.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<EdiAuditLog> all = auditLogRepository.findAll();
        Map<String, Long> byStatus = new java.util.LinkedHashMap<>();
        all.forEach(log -> byStatus.merge(log.getStatus().name(), 1L, Long::sum));
        return ResponseEntity.ok(Map.of(
                "totalRecords", (long) all.size(),
                "byStatus", byStatus
        ));
    }

    /**
     * Clears all audit log records — useful for resetting between test runs.
     */
    @DeleteMapping("/audit-log")
    public ResponseEntity<String> clearAll() {
        long count = auditLogRepository.count();
        auditLogRepository.deleteAll();
        return ResponseEntity.ok("Deleted " + count + " records.");
    }
}
