package com.nexaedi.core.service;

import com.nexaedi.core.mapping.MappingProfile;
import com.nexaedi.core.mapping.MappingRegistry;
import com.nexaedi.core.mapping.X12ToCanonicalMapper;
import com.nexaedi.core.model.CanonicalOrder;
import com.nexaedi.core.model.EdiProcessingStatus;
import com.nexaedi.core.model.X12Interchange;
import com.nexaedi.core.model.X12Transaction;
import com.nexaedi.core.parser.EdiParseException;
import com.nexaedi.core.parser.UniversalX12Parser;
import com.nexaedi.infrastructure.dlq.DeadLetterQueueService;
import com.nexaedi.infrastructure.shopify.ShopifyOutboundAdapter;
import com.nexaedi.infrastructure.shopify.ShopifyTransmissionException;
import com.nexaedi.infrastructure.storage.StorageService;
import com.nexaedi.portal.model.OrderSyncStatus;
import com.nexaedi.portal.model.PlatformType;
import com.nexaedi.portal.model.SellerOrder;
import com.nexaedi.portal.repository.SellerOrderRepository;
import com.nexaedi.portal.repository.SellerRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The central orchestrator of the NexaEDI processing pipeline.
 *
 * Pipeline stages for each EDI file:
 *  1. RECEIVED  — Store raw content in S3, write audit record
 *  2. PARSED    — Parse X12 envelope, apply MappingProfile, build CanonicalOrder
 *  3. VALIDATED — Run Hibernate Validator against the CanonicalOrder
 *  4. TRANSMITTED — Transmit to Shopify via ShopifyOutboundAdapter
 *  5. ACKNOWLEDGED — Write final success audit record
 *
 * Transaction Isolation: Each file is processed on its own Virtual Thread via @Async.
 * If one file in a batch fails, the exception is caught and quarantined to the DLQ;
 * other files continue processing independently.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EdiOrchestrationService {

    private final UniversalX12Parser parser;
    private final MappingRegistry mappingRegistry;
    private final X12ToCanonicalMapper mapper;
    private final Validator validator;
    private final AuditLoggingService auditLoggingService;
    private final DeadLetterQueueService dlqService;
    private final ShopifyOutboundAdapter shopifyAdapter;
    private final StorageService inboundStorageService;
    private final SellerRepository sellerRepository;
    private final SellerOrderRepository sellerOrderRepository;

    /**
     * Processes a single raw EDI file asynchronously on a Virtual Thread.
     * Failures are isolated: they trigger DLQ quarantine without affecting other concurrent files.
     *
     * @param retailerId   the originating retailer (e.g. "TARGET")
     * @param rawContent   the complete raw X12 EDI file content
     * @param fileName     the original file name (for DLQ error reports)
     * @return CompletableFuture resolving to the correlation ID on success
     */
    @Async("ediVirtualThreadExecutor")
    public CompletableFuture<String> processAsync(String correlationId, String retailerId,
                                                   String rawContent, String fileName, Long sellerId) {
        log.info("[ORCHESTRATOR] Starting pipeline — correlationId={} retailer={} sellerId={} file={}",
                correlationId, retailerId, sellerId, fileName);
        try {
            return CompletableFuture.completedFuture(
                    runPipeline(correlationId, retailerId, rawContent, fileName, sellerId));
        } catch (Exception e) {
            handlePipelineFailure(correlationId, retailerId, rawContent, fileName, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private String runPipeline(String correlationId, String retailerId, String rawContent, String fileName, Long sellerId) {
        long stageStart = System.currentTimeMillis();

        // Stage 1: RECEIVED — persist to S3 and audit
        String s3Key = storageService.storeInbound(correlationId, retailerId, rawContent);
        auditLoggingService.record(correlationId, retailerId, null, null,
                EdiProcessingStatus.RECEIVED, s3Key,
                "File received and stored in S3: " + s3Key,
                System.currentTimeMillis() - stageStart);

        // Stage 2: PARSED — parse X12 and map to CDM
        stageStart = System.currentTimeMillis();
        X12Interchange interchange = parser.parse(rawContent);

        X12Transaction transaction = interchange.getGroups().stream()
                .flatMap(g -> g.getTransactions().stream())
                .findFirst()
                .orElseThrow(() -> new EdiParseException("No ST transaction found in interchange", "ST", 0));

        String transactionSetCode = transaction.getTransactionSetCode();
        MappingProfile profile = mappingRegistry.find(retailerId, transactionSetCode)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("No mapping profile found for retailer '%s' and transaction '%s'. " +
                                "Drop a JSON file named %s-%s.json into the /mappings directory.",
                                retailerId, transactionSetCode,
                                retailerId.toLowerCase(), transactionSetCode)));

        CanonicalOrder canonicalOrder = mapper.map(transaction, profile, retailerId);
        canonicalOrder = CanonicalOrder.builder()
                .correlationId(correlationId)
                .retailerId(canonicalOrder.getRetailerId())
                .poNumber(canonicalOrder.getPoNumber())
                .purchaseOrderType(canonicalOrder.getPurchaseOrderType())
                .poDate(canonicalOrder.getPoDate())
                .requestedDeliveryDate(canonicalOrder.getRequestedDeliveryDate())
                .shipToName(canonicalOrder.getShipToName())
                .shipToAddress(canonicalOrder.getShipToAddress())
                .shipToCity(canonicalOrder.getShipToCity())
                .shipToState(canonicalOrder.getShipToState())
                .shipToZip(canonicalOrder.getShipToZip())
                .departmentNumber(canonicalOrder.getDepartmentNumber())
                .lines(canonicalOrder.getLines())
                .interchangeControlNumber(interchange.getControlNumber())
                .transactionControlNumber(transaction.getControlNumber())
                .build();

        auditLoggingService.record(correlationId, retailerId, transactionSetCode,
                canonicalOrder.getPoNumber(), EdiProcessingStatus.PARSED, s3Key,
                "Parsed " + canonicalOrder.getLines().size() + " line items from " + transactionSetCode + " transaction",
                System.currentTimeMillis() - stageStart);

        // Stage 3: VALIDATED — Hibernate Validator
        stageStart = System.currentTimeMillis();
        Set<ConstraintViolation<CanonicalOrder>> violations = validator.validate(canonicalOrder);
        if (!violations.isEmpty()) {
            String violationSummary = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("unknown");
            throw new IllegalStateException("Canonical order validation failed: " + violationSummary);
        }

        auditLoggingService.record(correlationId, retailerId, transactionSetCode,
                canonicalOrder.getPoNumber(), EdiProcessingStatus.VALIDATED, s3Key,
                "Validation passed — " + canonicalOrder.getLines().size() + " lines verified",
                System.currentTimeMillis() - stageStart);

        // Stage 4: TRANSMITTED — Send to Shopify
        stageStart = System.currentTimeMillis();
        String shopifyOrderId = shopifyAdapter.transmit(canonicalOrder);

        auditLoggingService.record(correlationId, retailerId, transactionSetCode,
                canonicalOrder.getPoNumber(), EdiProcessingStatus.TRANSMITTED, s3Key,
                "Successfully transmitted to Shopify. Draft Order ID: " + shopifyOrderId,
                System.currentTimeMillis() - stageStart);

        // Stage 5: ACKNOWLEDGED + create SellerOrder for portal visibility
        storageService.archiveProcessed(s3Key, correlationId);
        auditLoggingService.record(correlationId, retailerId, transactionSetCode,
                canonicalOrder.getPoNumber(), EdiProcessingStatus.ACKNOWLEDGED, s3Key,
                "Pipeline complete. Shopify Draft Order: " + shopifyOrderId, 0L);

        createSellerOrder(canonicalOrder, shopifyOrderId, correlationId, sellerId, transactionSetCode);

        log.info("[ORCHESTRATOR] Pipeline complete — correlationId={} poNumber={} shopifyOrderId={}",
                correlationId, canonicalOrder.getPoNumber(), shopifyOrderId);

        return correlationId;
    }

    private void createSellerOrder(CanonicalOrder order, String shopifyOrderId,
                                   String correlationId, Long sellerId, String txnSetCode) {
        if (sellerId == null) return;
        try {
            var seller = sellerRepository.findById(sellerId).orElse(null);
            if (seller == null) return;

            // Build line items JSON
            StringBuilder lineItems = new StringBuilder("[");
            for (int i = 0; i < order.getLines().size(); i++) {
                var l = order.getLines().get(i);
                if (i > 0) lineItems.append(",");
                lineItems.append(String.format(
                    "{\"description\":\"%s\",\"sku\":\"%s\",\"quantity\":%d,\"unitPrice\":%.2f,\"lineTotal\":%.2f}",
                    l.getSku(), l.getSku(), l.getQuantityOrdered(),
                    l.getUnitPrice().doubleValue(),
                    l.getUnitPrice().doubleValue() * l.getQuantityOrdered()
                ));
            }
            lineItems.append("]");

            java.math.BigDecimal total = order.getLines().stream()
                .map(l -> l.getUnitPrice().multiply(new java.math.BigDecimal(l.getQuantityOrdered())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            int totalUnits = order.getLines().stream().mapToInt(l -> l.getQuantityOrdered()).sum();

            SellerOrder so = SellerOrder.builder()
                .seller(seller)
                .retailerId(order.getRetailerId())
                .retailerDisplayName(order.getRetailerId())
                .poNumber(order.getPoNumber())
                .platformType(PlatformType.SHOPIFY)
                .platformOrderId(shopifyOrderId)
                .status(OrderSyncStatus.ACKNOWLEDGED)
                .orderValue(total)
                .currency("USD")
                .lineItemCount(order.getLines().size())
                .totalUnits(totalUnits)
                .shipToName(order.getShipToName())
                .shipToCity(order.getShipToCity())
                .shipToState(order.getShipToState())
                .requestedDeliveryDate(order.getRequestedDeliveryDate() != null
                    ? order.getRequestedDeliveryDate().toString() : null)
                .lineItemsJson(lineItems.toString())
                .correlationId(correlationId)
                .receivedAt(java.time.Instant.now())
                .syncedAt(java.time.Instant.now())
                .build();

            sellerOrderRepository.save(so);
            log.info("[ORCHESTRATOR] SellerOrder created for sellerId={} poNumber={}", sellerId, order.getPoNumber());
        } catch (Exception e) {
            log.warn("[ORCHESTRATOR] Failed to create SellerOrder (non-critical): {}", e.getMessage());
        }
    }

    private void handlePipelineFailure(String correlationId, String retailerId, String rawContent,
                                       String fileName, Exception e) {
        log.error("[ORCHESTRATOR] Pipeline FAILED — correlationId={} retailer={} error={}",
                correlationId, retailerId, e.getMessage(), e);

        dlqService.quarantine(correlationId, retailerId, rawContent, fileName,
                "Pipeline failure: " + e.getMessage(), e);

        auditLoggingService.recordFailure(correlationId, retailerId, fileName,
                "Processing failed: " + e.getMessage(),
                e.getClass().getName() + ": " + e.getMessage());
    }
}
