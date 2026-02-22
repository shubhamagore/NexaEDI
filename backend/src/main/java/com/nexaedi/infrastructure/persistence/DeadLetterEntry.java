package com.nexaedi.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "dead_letter_entry",
    indexes = @Index(name = "idx_dlq_correlation_id", columnList = "correlation_id")
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "retailer_id", nullable = false, length = 50)
    private String retailerId;

    @Column(name = "correlation_id", nullable = false, length = 36)
    private String correlationId;

    @Lob
    @Column(name = "original_content", columnDefinition = "TEXT", nullable = false)
    private String originalContent;

    @Lob
    @Column(name = "error_report", columnDefinition = "TEXT", nullable = false)
    private String errorReport;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
