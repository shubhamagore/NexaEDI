package com.nexaedi.core.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single parsed X12 segment, e.g. "BEG*00*SA*PO-12345**20260101"
 * Segment ID is the first element (index 0); data elements follow from index 1.
 */
@Getter
public class X12Segment {

    private final String segmentId;
    private final List<String> elements;
    private final int lineNumber;

    public X12Segment(String rawSegment, char elementDelimiter, int lineNumber) {
        String[] parts = rawSegment.split(java.util.regex.Pattern.quote(String.valueOf(elementDelimiter)), -1);
        this.segmentId = parts[0].trim();
        this.elements = Collections.unmodifiableList(Arrays.asList(parts).subList(1, parts.length));
        this.lineNumber = lineNumber;
    }

    /**
     * Returns element at 1-based position (matching X12 notation: BEG03 = position 3).
     * Returns empty string if position is out of range.
     */
    public String getElement(int position) {
        int index = position - 1;
        if (index < 0 || index >= elements.size()) {
            return "";
        }
        return elements.get(index);
    }

    /**
     * Returns a qualified element reference in X12 notation, e.g. "BEG03".
     */
    public String qualifiedRef(int position) {
        return segmentId + String.format("%02d", position);
    }

    @Override
    public String toString() {
        return segmentId + "*" + String.join("*", elements);
    }
}
