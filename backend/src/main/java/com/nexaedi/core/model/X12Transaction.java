package com.nexaedi.core.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an X12 Transaction Set, bounded by ST...SE envelope segments.
 * Example: a single 850 Purchase Order transaction.
 */
@Getter
public class X12Transaction {

    private final String transactionSetCode;
    private final String controlNumber;
    private final List<X12Segment> segments;

    public X12Transaction(String transactionSetCode, String controlNumber) {
        this.transactionSetCode = transactionSetCode;
        this.controlNumber = controlNumber;
        this.segments = new ArrayList<>();
    }

    public void addSegment(X12Segment segment) {
        segments.add(segment);
    }

    public List<X12Segment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    /**
     * Finds the first segment matching the given segment ID.
     */
    public X12Segment findFirst(String segmentId) {
        return segments.stream()
                .filter(s -> s.getSegmentId().equals(segmentId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns all segments matching the given segment ID (e.g., all PO1 line items).
     */
    public List<X12Segment> findAll(String segmentId) {
        return segments.stream()
                .filter(s -> s.getSegmentId().equals(segmentId))
                .toList();
    }
}
