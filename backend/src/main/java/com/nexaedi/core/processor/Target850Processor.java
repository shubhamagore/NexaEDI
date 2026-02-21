package com.nexaedi.core.processor;

import com.nexaedi.core.mapping.MappingProfile;
import com.nexaedi.core.mapping.MappingRegistry;
import com.nexaedi.core.mapping.X12ToCanonicalMapper;
import com.nexaedi.core.model.CanonicalOrder;
import com.nexaedi.core.model.EdiProcessingStatus;
import com.nexaedi.core.model.X12Interchange;
import com.nexaedi.core.model.X12Transaction;
import com.nexaedi.core.parser.UniversalX12Parser;
import com.nexaedi.core.service.AuditLoggingService;
import com.nexaedi.infrastructure.dlq.DeadLetterQueueService;
import com.nexaedi.infrastructure.shopify.ShopifyOutboundAdapter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Demonstrates the first end-to-end EDI processing flow for Target 850 Purchase Orders.
 *
 * This class is a self-contained, readable demonstration of the full NexaEDI pipeline:
 *
 *   RAW X12 EDI
 *       │
 *       ▼
 *   [UniversalX12Parser]   — Parses ISA/GS/ST envelope into structured objects
 *       │
 *       ▼
 *   [MappingRegistry]      — Loads "TARGET:850" profile from target-850.json
 *       │
 *       ▼
 *   [X12ToCanonicalMapper] — Applies JSON rules; produces CanonicalOrder (CDM)
 *       │
 *       ▼
 *   [Hibernate Validator]  — Enforces @NotBlank, @Positive, @NotEmpty constraints
 *       │
 *       ▼
 *   [ShopifyOutboundAdapter] — POSTs draft order to Shopify 2026-01 API
 *       │                      with leaky-bucket rate limiting + Spring Retry
 *       ▼
 *   [AuditLoggingService]  — Records every lifecycle transition to PostgreSQL
 *       │
 *  (on failure)
 *       ▼
 *   [DeadLetterQueueService] — Quarantines file + writes .error report to /dlq
 *
 * Sample X12 850 used below represents a real-world Target PO with two line items.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Target850Processor {

    private static final String RETAILER_ID = "TARGET";

    private final UniversalX12Parser parser;
    private final MappingRegistry mappingRegistry;
    private final X12ToCanonicalMapper mapper;
    private final Validator validator;
    private final AuditLoggingService auditLoggingService;
    private final ShopifyOutboundAdapter shopifyAdapter;
    private final DeadLetterQueueService dlqService;

    /**
     * Sample Target 850 EDI document for demonstration purposes.
     *
     * Segment breakdown:
     *  ISA  — Interchange header (Target → Vendor)
     *  GS   — Functional group (Purchase Orders)
     *  ST   — Transaction set start (850)
     *  BEG  — Beginning of PO: Type=SA, PO#=TGT-2026-00042, Date=2026-02-19
     *  REF  — Department: DP / 042
     *  DTM  — Delivery date requested: 2026-03-05
     *  N1   — Ship-to: ST (Store #1742 Minneapolis)
     *  N3   — Ship-to street address
     *  N4   — Ship-to city/state/zip
     *  PO1  — Line 1: Qty=120, EA, $24.99, SKU=089541234567
     *  PO1  — Line 2: Qty=60,  EA, $49.99, SKU=089599876543
     *  CTT  — Transaction totals
     *  SE   — Transaction set end
     *  GE   — Group end
     *  IEA  — Interchange end
     */
    public static final String SAMPLE_TARGET_850 =
            "ISA*00*          *00*          *ZZ*TARGET         *ZZ*VENDORABC      *260219*1200*^*00501*000000042*0*P*>~" +
            "GS*PO*TGTBUY*VENDORABC*20260219*1200*42*X*005010~" +
            "ST*850*0001~" +
            "BEG*00*SA*TGT-2026-00042**20260219~" +
            "REF*DP*042~" +
            "DTM*002*20260305~" +
            "N1*ST*Target Store #1742*92*1742~" +
            "N3*700 Nicollet Mall~" +
            "N4*Minneapolis*MN*55402~" +
            "PO1*1*120*EA*24.99**UI*089541234567~" +
            "PO1*2*60*EA*49.99**UI*089599876543~" +
            "CTT*2~" +
            "SE*11*0001~" +
            "GE*1*42~" +
            "IEA*1*000000042~";

    /**
     * Executes the complete end-to-end pipeline for the sample Target 850.
     * Call this method from a Spring ApplicationRunner, integration test, or CLI tool.
     *
     * @return the CanonicalOrder produced after successful parsing and validation
     */
    public CanonicalOrder processDemo() {
        String correlationId = UUID.randomUUID().toString();
        log.info("=== Target850Processor Demo START — correlationId={} ===", correlationId);

        try {
            // ── Step 1: Parse raw X12 into envelope structure ─────────────────────
            log.info("[Step 1] Parsing X12 interchange...");
            X12Interchange interchange = parser.parse(SAMPLE_TARGET_850);
            log.info("[Step 1] ✓ Parsed ISA envelope: sender={}, receiver={}, controlNumber={}",
                    interchange.getSenderId(), interchange.getReceiverId(), interchange.getControlNumber());

            // ── Step 2: Extract the first 850 transaction ─────────────────────────
            X12Transaction transaction = interchange.getGroups().stream()
                    .flatMap(g -> g.getTransactions().stream())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No ST transaction found"));

            log.info("[Step 2] ✓ Transaction set: code={}, controlNumber={}, segments={}",
                    transaction.getTransactionSetCode(),
                    transaction.getControlNumber(),
                    transaction.getSegments().size());

            // ── Step 3: Load the TARGET:850 MappingProfile ────────────────────────
            log.info("[Step 3] Loading mapping profile for TARGET:850...");
            MappingProfile profile = mappingRegistry.find(RETAILER_ID, "850")
                    .orElseThrow(() -> new IllegalStateException(
                            "Mapping profile 'target-850.json' not loaded. " +
                            "Ensure the file is in the /mappings directory and the service has been restarted."));
            log.info("[Step 3] ✓ Loaded profile v{}: {}", profile.getVersion(), profile.getDescription());

            // ── Step 4: Map X12 → Canonical Data Model ────────────────────────────
            log.info("[Step 4] Applying mapping rules to build CanonicalOrder...");
            CanonicalOrder order = mapper.map(transaction, profile, RETAILER_ID);
            order = rebuildWithCorrelationId(order, correlationId, interchange, transaction);

            log.info("[Step 4] ✓ CanonicalOrder built: poNumber={}, lines={}, shipTo={}",
                    order.getPoNumber(), order.getLines().size(), order.getShipToName());

            order.getLines().forEach(line ->
                    log.info("         Line {}: sku={} qty={} @${} {}",
                            line.getLineSequenceNumber(), line.getSku(),
                            line.getQuantityOrdered(), line.getUnitPrice(), line.getUnitOfMeasure()));

            // ── Step 5: Hibernate Validation ──────────────────────────────────────
            log.info("[Step 5] Validating CanonicalOrder with Hibernate Validator...");
            Set<ConstraintViolation<CanonicalOrder>> violations = validator.validate(order);
            if (!violations.isEmpty()) {
                violations.forEach(v ->
                        log.error("[Step 5] ✗ Violation: {} → {}", v.getPropertyPath(), v.getMessage()));
                throw new IllegalStateException("Validation failed: " + violations.size() + " constraint violation(s)");
            }
            log.info("[Step 5] ✓ All {} constraint(s) satisfied", countConstraints(order));

            // ── Step 6: Audit — PARSED ─────────────────────────────────────────────
            auditLoggingService.record(correlationId, RETAILER_ID, "850", order.getPoNumber(),
                    EdiProcessingStatus.PARSED, "demo/sample-target-850.edi",
                    "Demo pipeline: parsed and validated successfully", null);

            // ── Step 7: Transmit to Shopify ────────────────────────────────────────
            log.info("[Step 7] Transmitting to Shopify Admin API...");
            String shopifyOrderId = shopifyAdapter.transmit(order);
            log.info("[Step 7] ✓ Shopify Draft Order created: {}", shopifyOrderId);

            // ── Step 8: Audit — TRANSMITTED ────────────────────────────────────────
            auditLoggingService.record(correlationId, RETAILER_ID, "850", order.getPoNumber(),
                    EdiProcessingStatus.TRANSMITTED, "demo/sample-target-850.edi",
                    "Transmitted to Shopify. Draft Order ID: " + shopifyOrderId, null);

            log.info("=== Target850Processor Demo COMPLETE — poNumber={} shopifyOrderId={} ===",
                    order.getPoNumber(), shopifyOrderId);

            return order;

        } catch (Exception e) {
            log.error("=== Target850Processor Demo FAILED — correlationId={} error={} ===",
                    correlationId, e.getMessage(), e);

            dlqService.quarantine(correlationId, RETAILER_ID, SAMPLE_TARGET_850,
                    "demo-target-850.edi", "Demo pipeline failure: " + e.getMessage(), e);

            auditLoggingService.recordFailure(correlationId, RETAILER_ID,
                    "demo/sample-target-850.edi", e.getMessage(), e.toString());

            throw new RuntimeException("Target850 demo pipeline failed", e);
        }
    }

    private CanonicalOrder rebuildWithCorrelationId(CanonicalOrder order, String correlationId,
                                                     X12Interchange interchange, X12Transaction transaction) {
        return CanonicalOrder.builder()
                .correlationId(correlationId)
                .retailerId(order.getRetailerId())
                .poNumber(order.getPoNumber())
                .purchaseOrderType(order.getPurchaseOrderType())
                .poDate(order.getPoDate())
                .requestedDeliveryDate(order.getRequestedDeliveryDate())
                .shipToName(order.getShipToName())
                .shipToAddress(order.getShipToAddress())
                .shipToCity(order.getShipToCity())
                .shipToState(order.getShipToState())
                .shipToZip(order.getShipToZip())
                .departmentNumber(order.getDepartmentNumber())
                .lines(order.getLines())
                .interchangeControlNumber(interchange.getControlNumber())
                .transactionControlNumber(transaction.getControlNumber())
                .build();
    }

    private int countConstraints(CanonicalOrder order) {
        return 7 + (order.getLines().size() * 5);
    }
}
