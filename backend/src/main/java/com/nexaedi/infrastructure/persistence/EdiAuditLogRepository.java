package com.nexaedi.infrastructure.persistence;

import com.nexaedi.core.model.EdiProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for querying the EDI audit trail.
 */
@Repository
public interface EdiAuditLogRepository extends JpaRepository<EdiAuditLog, Long> {

    List<EdiAuditLog> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);

    List<EdiAuditLog> findByRetailerIdAndStatus(String retailerId, EdiProcessingStatus status);

    List<EdiAuditLog> findByPoNumber(String poNumber);

    long countByStatus(EdiProcessingStatus status);
}
