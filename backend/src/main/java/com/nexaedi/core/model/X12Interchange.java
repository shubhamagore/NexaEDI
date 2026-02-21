package com.nexaedi.core.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a complete X12 Interchange, bounded by ISA...IEA envelope segments.
 * This is the outermost container of an EDI file and may contain multiple Functional Groups.
 */
@Getter
public class X12Interchange {

    private final String senderId;
    private final String receiverId;
    private final String interchangeDate;
    private final String interchangeTime;
    private final String controlNumber;
    private final char elementDelimiter;
    private final char componentDelimiter;
    private final char segmentTerminator;
    private final List<X12Group> groups;

    public X12Interchange(String senderId, String receiverId, String interchangeDate,
                          String interchangeTime, String controlNumber,
                          char elementDelimiter, char componentDelimiter, char segmentTerminator) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.interchangeDate = interchangeDate;
        this.interchangeTime = interchangeTime;
        this.controlNumber = controlNumber;
        this.elementDelimiter = elementDelimiter;
        this.componentDelimiter = componentDelimiter;
        this.segmentTerminator = segmentTerminator;
        this.groups = new ArrayList<>();
    }

    public void addGroup(X12Group group) {
        groups.add(group);
    }

    public List<X12Group> getGroups() {
        return Collections.unmodifiableList(groups);
    }
}
