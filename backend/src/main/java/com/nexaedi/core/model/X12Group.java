package com.nexaedi.core.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an X12 Functional Group, bounded by GS...GE envelope segments.
 * A single group can contain multiple transaction sets of the same type.
 */
@Getter
public class X12Group {

    private final String functionalIdentifierCode;
    private final String senderApplicationCode;
    private final String receiverApplicationCode;
    private final String groupControlNumber;
    private final List<X12Transaction> transactions;

    public X12Group(String functionalIdentifierCode, String senderApplicationCode,
                    String receiverApplicationCode, String groupControlNumber) {
        this.functionalIdentifierCode = functionalIdentifierCode;
        this.senderApplicationCode = senderApplicationCode;
        this.receiverApplicationCode = receiverApplicationCode;
        this.groupControlNumber = groupControlNumber;
        this.transactions = new ArrayList<>();
    }

    public void addTransaction(X12Transaction transaction) {
        transactions.add(transaction);
    }

    public List<X12Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }
}
