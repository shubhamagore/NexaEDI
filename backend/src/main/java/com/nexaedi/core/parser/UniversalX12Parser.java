package com.nexaedi.core.parser;

import com.nexaedi.core.model.X12Group;
import com.nexaedi.core.model.X12Interchange;
import com.nexaedi.core.model.X12Segment;
import com.nexaedi.core.model.X12Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses raw X12 EDI content into a structured {@link X12Interchange} object tree,
 * handling ISA/GS/ST envelope nesting and segment loops.
 *
 * The parser is stateless and thread-safe; it is safe to share across virtual threads.
 *
 * Parsing strategy:
 *  1. Read the ISA segment (always exactly 106 characters) to extract delimiters.
 *  2. Split the remaining content by the segment terminator.
 *  3. Walk segments sequentially, maintaining envelope state (ISA > GS > ST).
 */
@Slf4j
@Component
public class UniversalX12Parser {

    private static final int ISA_LENGTH = 106;
    private static final String ISA = "ISA";
    private static final String IEA = "IEA";
    private static final String GS = "GS";
    private static final String GE = "GE";
    private static final String ST = "ST";
    private static final String SE = "SE";

    /**
     * Parses the raw EDI string content into an X12Interchange object.
     *
     * @param rawContent the complete text content of the EDI file
     * @return a fully structured X12Interchange
     * @throws EdiParseException if envelope structure is invalid or required segments are missing
     */
    public X12Interchange parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new EdiParseException("EDI content is empty or null", ISA, 0);
        }

        String normalized = rawContent.replace("\r\n", "\n").replace("\r", "\n");

        if (normalized.length() < ISA_LENGTH) {
            throw new EdiParseException(
                    "Content too short to contain a valid ISA segment (min 106 chars)", ISA, 1);
        }

        char elementDelimiter = normalized.charAt(3);
        char componentDelimiter = normalized.charAt(104);
        char segmentTerminator = normalized.charAt(105);

        log.debug("Detected delimiters — element: '{}', component: '{}', segment terminator: '{}'",
                elementDelimiter, componentDelimiter, (int) segmentTerminator);

        List<String> rawSegments = splitByTerminator(normalized, segmentTerminator);
        List<X12Segment> segments = parseSegments(rawSegments, elementDelimiter);

        return buildInterchange(segments, elementDelimiter, componentDelimiter, segmentTerminator);
    }

    private List<String> splitByTerminator(String content, char terminator) {
        List<String> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (char c : content.toCharArray()) {
            if (c == terminator) {
                String seg = buffer.toString().trim();
                if (!seg.isBlank()) {
                    result.add(seg);
                }
                buffer.setLength(0);
            } else {
                buffer.append(c);
            }
        }
        if (!buffer.toString().isBlank()) {
            result.add(buffer.toString().trim());
        }
        return result;
    }

    private List<X12Segment> parseSegments(List<String> rawSegments, char elementDelimiter) {
        List<X12Segment> segments = new ArrayList<>();
        for (int i = 0; i < rawSegments.size(); i++) {
            String raw = rawSegments.get(i);
            if (!raw.isBlank()) {
                segments.add(new X12Segment(raw, elementDelimiter, i + 1));
            }
        }
        return segments;
    }

    private X12Interchange buildInterchange(List<X12Segment> segments, char elementDelimiter,
                                            char componentDelimiter, char segmentTerminator) {
        X12Interchange interchange = null;
        X12Group currentGroup = null;
        X12Transaction currentTransaction = null;

        for (X12Segment seg : segments) {
            switch (seg.getSegmentId()) {
                case ISA -> {
                    validateMinElements(seg, 16);
                    interchange = new X12Interchange(
                            seg.getElement(6).trim(),
                            seg.getElement(8).trim(),
                            seg.getElement(9),
                            seg.getElement(10),
                            seg.getElement(13),
                            elementDelimiter,
                            componentDelimiter,
                            segmentTerminator
                    );
                }
                case GS -> {
                    requireInterchange(interchange, seg);
                    validateMinElements(seg, 8);
                    currentGroup = new X12Group(
                            seg.getElement(1),
                            seg.getElement(2),
                            seg.getElement(3),
                            seg.getElement(6)
                    );
                }
                case ST -> {
                    requireGroup(currentGroup, seg);
                    validateMinElements(seg, 2);
                    currentTransaction = new X12Transaction(
                            seg.getElement(1),
                            seg.getElement(2)
                    );
                }
                case SE -> {
                    requireTransaction(currentTransaction, seg);
                    requireGroup(currentGroup, seg);
                    currentGroup.addTransaction(currentTransaction);
                    currentTransaction = null;
                }
                case GE -> {
                    requireGroup(currentGroup, seg);
                    requireInterchange(interchange, seg);
                    interchange.addGroup(currentGroup);
                    currentGroup = null;
                }
                case IEA -> {
                    requireInterchange(interchange, seg);
                }
                default -> {
                    if (currentTransaction != null) {
                        currentTransaction.addSegment(seg);
                    }
                }
            }
        }

        if (interchange == null) {
            throw new EdiParseException("No ISA segment found in EDI content", ISA, 0);
        }

        return interchange;
    }

    private void validateMinElements(X12Segment seg, int minCount) {
        if (seg.getElements().size() < minCount) {
            throw new EdiParseException(
                    String.format("Expected at least %d elements but found %d",
                            minCount, seg.getElements().size()),
                    seg.getSegmentId(), seg.getLineNumber());
        }
    }

    private void requireInterchange(X12Interchange interchange, X12Segment seg) {
        if (interchange == null) {
            throw new EdiParseException(
                    "Encountered segment outside of ISA envelope", seg.getSegmentId(), seg.getLineNumber());
        }
    }

    private void requireGroup(X12Group group, X12Segment seg) {
        if (group == null) {
            throw new EdiParseException(
                    "Encountered segment outside of GS envelope", seg.getSegmentId(), seg.getLineNumber());
        }
    }

    private void requireTransaction(X12Transaction transaction, X12Segment seg) {
        if (transaction == null) {
            throw new EdiParseException(
                    "Encountered SE without a matching ST segment", seg.getSegmentId(), seg.getLineNumber());
        }
    }
}
