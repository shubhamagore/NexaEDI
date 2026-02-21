package com.nexaedi.core.mapping;

import com.nexaedi.core.model.CanonicalOrder;
import com.nexaedi.core.model.CanonicalOrderLine;
import com.nexaedi.core.model.X12Segment;
import com.nexaedi.core.model.X12Transaction;
import com.nexaedi.core.parser.EdiParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Applies a {@link MappingProfile} to an {@link X12Transaction} to produce a {@link CanonicalOrder}.
 * This is the heart of the "mapping-first" architecture: all retailer-specific translation
 * logic lives in the JSON profile, not in Java code.
 */
@Slf4j
@Component
public class X12ToCanonicalMapper {

    private static final DateTimeFormatter EDI_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Translates a parsed X12 transaction into a Canonical Order using the supplied profile.
     *
     * @param transaction the parsed X12 transaction (ST...SE)
     * @param profile     the mapping profile for this retailer and transaction type
     * @param retailerId  the originating retailer identifier
     * @return a fully populated CanonicalOrder
     */
    public CanonicalOrder map(X12Transaction transaction, MappingProfile profile, String retailerId) {
        log.debug("Mapping transaction {} using profile {}:{}",
                transaction.getControlNumber(), profile.getRetailerId(), profile.getTransactionSetCode());

        CanonicalOrder.CanonicalOrderBuilder builder = CanonicalOrder.builder()
                .correlationId(UUID.randomUUID().toString())
                .retailerId(retailerId.toUpperCase())
                .transactionControlNumber(transaction.getControlNumber());

        applyHeaderMappings(transaction, profile, builder);
        List<CanonicalOrderLine> lines = applyLineMappings(transaction, profile);
        builder.lines(lines);

        return builder.build();
    }

    private void applyHeaderMappings(X12Transaction transaction, MappingProfile profile,
                                     CanonicalOrder.CanonicalOrderBuilder builder) {
        for (MappingRule rule : profile.getHeaderMappings()) {
            X12Segment segment = resolveSegment(transaction, rule);
            if (segment == null) {
                if (rule.isRequired()) {
                    throw new EdiParseException(
                            "Required segment '" + rule.getSegmentId() + "' not found in transaction",
                            rule.getSegmentId(), 0);
                }
                continue;
            }

            String value = segment.getElement(rule.getElementPosition());
            if ((value == null || value.isBlank()) && rule.getDefaultValue() != null) {
                value = rule.getDefaultValue();
            }
            if ((value == null || value.isBlank()) && rule.isRequired()) {
                throw new EdiParseException(
                        String.format("Required element %s%02d is empty",
                                rule.getSegmentId(), rule.getElementPosition()),
                        rule.getSegmentId(), segment.getLineNumber());
            }

            applyHeaderField(builder, rule.getTargetField(), value, segment);
        }
    }

    private void applyHeaderField(CanonicalOrder.CanonicalOrderBuilder builder,
                                  String targetField, String value, X12Segment segment) {
        switch (targetField) {
            case "poNumber"              -> builder.poNumber(value);
            case "purchaseOrderType"     -> builder.purchaseOrderType(value);
            case "poDate"                -> builder.poDate(parseDate(value, segment));
            case "requestedDeliveryDate" -> builder.requestedDeliveryDate(parseDate(value, segment));
            case "shipToName"            -> builder.shipToName(value);
            case "shipToAddress"         -> builder.shipToAddress(value);
            case "shipToCity"            -> builder.shipToCity(value);
            case "shipToState"           -> builder.shipToState(value);
            case "shipToZip"             -> builder.shipToZip(value);
            case "departmentNumber"      -> builder.departmentNumber(value);
            default -> log.warn("Unknown header target field '{}' in mapping profile — skipping", targetField);
        }
    }

    private List<CanonicalOrderLine> applyLineMappings(X12Transaction transaction, MappingProfile profile) {
        List<X12Segment> po1Segments = transaction.findAll("PO1");
        List<CanonicalOrderLine> lines = new ArrayList<>();

        for (int i = 0; i < po1Segments.size(); i++) {
            X12Segment po1 = po1Segments.get(i);
            CanonicalOrderLine.CanonicalOrderLineBuilder lineBuilder = CanonicalOrderLine.builder()
                    .lineSequenceNumber(i + 1);

            for (MappingRule rule : profile.getLineMappings()) {
                String value = po1.getElement(rule.getElementPosition());
                if ((value == null || value.isBlank()) && rule.getDefaultValue() != null) {
                    value = rule.getDefaultValue();
                }
                if ((value == null || value.isBlank()) && rule.isRequired()) {
                    throw new EdiParseException(
                            String.format("Required line-level element PO1%02d is empty on line sequence %d",
                                    rule.getElementPosition(), i + 1),
                            "PO1", po1.getLineNumber());
                }
                applyLineField(lineBuilder, rule.getTargetField(), value, po1);
            }

            lines.add(lineBuilder.build());
        }

        return lines;
    }

    private void applyLineField(CanonicalOrderLine.CanonicalOrderLineBuilder lineBuilder,
                                String targetField, String value, X12Segment segment) {
        switch (targetField) {
            case "quantityOrdered"   -> lineBuilder.quantityOrdered(parseIntSafe(value, segment));
            case "unitOfMeasure"     -> lineBuilder.unitOfMeasure(value);
            case "unitPrice"         -> lineBuilder.unitPrice(parseBigDecimalSafe(value, segment));
            case "sku"               -> lineBuilder.sku(value);
            case "productDescription"-> lineBuilder.productDescription(value);
            default -> log.warn("Unknown line-level target field '{}' in mapping profile — skipping", targetField);
        }
    }

    private X12Segment resolveSegment(X12Transaction transaction, MappingRule rule) {
        if (rule.getQualifier() == null || rule.getQualifier().isBlank()) {
            return transaction.findFirst(rule.getSegmentId());
        }

        String[] qualifierParts = rule.getQualifier().split(":");
        if (qualifierParts.length != 2) {
            return transaction.findFirst(rule.getSegmentId());
        }

        int qualPos = Integer.parseInt(qualifierParts[0]);
        String qualValue = qualifierParts[1];

        return transaction.findAll(rule.getSegmentId()).stream()
                .filter(s -> qualValue.equalsIgnoreCase(s.getElement(qualPos)))
                .findFirst()
                .orElse(null);
    }

    private LocalDate parseDate(String value, X12Segment segment) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, EDI_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new EdiParseException(
                    "Invalid date format '" + value + "' — expected yyyyMMdd",
                    segment.getSegmentId(), segment.getLineNumber(), e);
        }
    }

    private Integer parseIntSafe(String value, X12Segment segment) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new EdiParseException(
                    "Expected integer but got '" + value + "'",
                    segment.getSegmentId(), segment.getLineNumber(), e);
        }
    }

    private BigDecimal parseBigDecimalSafe(String value, X12Segment segment) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new EdiParseException(
                    "Expected decimal number but got '" + value + "'",
                    segment.getSegmentId(), segment.getLineNumber(), e);
        }
    }
}
