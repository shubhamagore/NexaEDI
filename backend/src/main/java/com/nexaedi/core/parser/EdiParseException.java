package com.nexaedi.core.parser;

/**
 * Thrown when the X12 parser encounters a malformed or unrecoverable segment/element.
 * Includes the segment ID and line number to allow precise Dead Letter Queue error logging.
 */
public class EdiParseException extends RuntimeException {

    private final String segmentId;
    private final int lineNumber;

    public EdiParseException(String message, String segmentId, int lineNumber) {
        super(String.format("[Line %d | Segment: %s] %s", lineNumber, segmentId, message));
        this.segmentId = segmentId;
        this.lineNumber = lineNumber;
    }

    public EdiParseException(String message, String segmentId, int lineNumber, Throwable cause) {
        super(String.format("[Line %d | Segment: %s] %s", lineNumber, segmentId, message), cause);
        this.segmentId = segmentId;
        this.lineNumber = lineNumber;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
