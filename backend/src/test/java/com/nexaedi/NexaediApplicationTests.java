package com.nexaedi;

import com.nexaedi.core.mapping.MappingRegistry;
import com.nexaedi.core.mapping.X12ToCanonicalMapper;
import com.nexaedi.core.model.CanonicalOrder;
import com.nexaedi.core.model.EdiProcessingStatus;
import com.nexaedi.core.model.X12Interchange;
import com.nexaedi.core.model.X12Transaction;
import com.nexaedi.core.parser.UniversalX12Parser;
import com.nexaedi.core.processor.Target850Processor;
import com.nexaedi.infrastructure.persistence.EdiAuditLog;
import com.nexaedi.infrastructure.persistence.EdiAuditLogRepository;
import com.nexaedi.infrastructure.shopify.ShopifyOutboundAdapter;
import com.nexaedi.infrastructure.s3.S3StorageService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Spring Boot integration tests.
 *
 * - H2 in-memory database replaces PostgreSQL (via application-test.yml)
 * - S3Client and ShopifyOutboundAdapter are mocked — no AWS or Shopify credentials needed
 * - MappingRegistry loads real JSON files from src/main/resources/mappings
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NexaEDI Spring Integration Tests")
class NexaediApplicationTests {

    // Mock the two external service dependencies so no real connections are made
    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3StorageService s3StorageService;

    @MockitoBean
    private ShopifyOutboundAdapter shopifyOutboundAdapter;

    @Autowired
    private UniversalX12Parser parser;

    @Autowired
    private MappingRegistry mappingRegistry;

    @Autowired
    private X12ToCanonicalMapper mapper;

    @Autowired
    private Validator validator;

    @Autowired
    private EdiAuditLogRepository auditLogRepository;

    @Test
    @DisplayName("Spring context loads successfully with mocked external services")
    void contextLoads() {
    }

    @Test
    @DisplayName("MappingRegistry should have TARGET:850 and WALMART:850 loaded")
    void mappingRegistryShouldHaveBothProfiles() {
        assertThat(mappingRegistry.find("TARGET", "850")).isPresent();
        assertThat(mappingRegistry.find("WALMART", "850")).isPresent();
    }

    @Test
    @DisplayName("Full parse → map → validate pipeline should pass without errors")
    void fullParseMapValidatePipelineShouldSucceed() {
        X12Interchange interchange = parser.parse(Target850Processor.SAMPLE_TARGET_850);
        X12Transaction transaction = interchange.getGroups().get(0).getTransactions().get(0);
        var profile = mappingRegistry.find("TARGET", "850").orElseThrow();

        CanonicalOrder order = mapper.map(transaction, profile, "TARGET");

        var violations = validator.validate(order);
        assertThat(violations)
                .as("Canonical order should pass all validation constraints")
                .isEmpty();
    }

    @Test
    @DisplayName("CanonicalOrder from sample 850 should have correct PO number and 2 lines")
    void canonicalOrderShouldHaveCorrectData() {
        X12Interchange interchange = parser.parse(Target850Processor.SAMPLE_TARGET_850);
        X12Transaction transaction = interchange.getGroups().get(0).getTransactions().get(0);
        var profile = mappingRegistry.find("TARGET", "850").orElseThrow();

        CanonicalOrder order = mapper.map(transaction, profile, "TARGET");

        assertThat(order.getPoNumber()).isEqualTo("TGT-2026-00042");
        assertThat(order.getShipToName()).isEqualTo("Target Store #1742");
        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getLines().get(0).getSku()).isEqualTo("089541234567");
        assertThat(order.getLines().get(1).getSku()).isEqualTo("089599876543");
    }

    @Test
    @DisplayName("EdiAuditLogRepository should save and retrieve audit records via H2")
    void auditRepositoryShouldSaveAndRetrieve() {
        EdiAuditLog log = EdiAuditLog.builder()
                .correlationId("test-corr-001")
                .retailerId("TARGET")
                .transactionSetCode("850")
                .poNumber("TGT-2026-00042")
                .status(EdiProcessingStatus.RECEIVED)
                .sourceFilePath("s3://nexaedi-test/edi/inbound/test-corr-001.edi")
                .message("Test audit entry")
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(log);

        List<EdiAuditLog> found = auditLogRepository.findByCorrelationIdOrderByCreatedAtAsc("test-corr-001");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getPoNumber()).isEqualTo("TGT-2026-00042");
        assertThat(found.get(0).getStatus()).isEqualTo(EdiProcessingStatus.RECEIVED);
    }

    @Test
    @DisplayName("EdiAuditLogRepository should count records by status")
    void auditRepositoryShouldCountByStatus() {
        auditLogRepository.deleteAll();

        auditLogRepository.save(buildLog("corr-001", EdiProcessingStatus.TRANSMITTED));
        auditLogRepository.save(buildLog("corr-002", EdiProcessingStatus.TRANSMITTED));
        auditLogRepository.save(buildLog("corr-003", EdiProcessingStatus.FAILED));

        assertThat(auditLogRepository.countByStatus(EdiProcessingStatus.TRANSMITTED)).isEqualTo(2);
        assertThat(auditLogRepository.countByStatus(EdiProcessingStatus.FAILED)).isEqualTo(1);
        assertThat(auditLogRepository.countByStatus(EdiProcessingStatus.RECEIVED)).isEqualTo(0);
    }

    private EdiAuditLog buildLog(String correlationId, EdiProcessingStatus status) {
        return EdiAuditLog.builder()
                .correlationId(correlationId)
                .retailerId("TARGET")
                .status(status)
                .createdAt(Instant.now())
                .build();
    }
}
