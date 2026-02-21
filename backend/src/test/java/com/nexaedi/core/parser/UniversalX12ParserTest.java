package com.nexaedi.core.parser;

import com.nexaedi.core.model.X12Interchange;
import com.nexaedi.core.model.X12Segment;
import com.nexaedi.core.model.X12Transaction;
import com.nexaedi.core.processor.Target850Processor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for the X12 parser.
 * No Spring context, no database, no network — runs in milliseconds.
 */
@DisplayName("UniversalX12Parser")
class UniversalX12ParserTest {

    private UniversalX12Parser parser;

    @BeforeEach
    void setUp() {
        parser = new UniversalX12Parser();
    }

    @Nested
    @DisplayName("ISA Envelope Parsing")
    class IsaEnvelopeParsing {

        @Test
        @DisplayName("should extract sender and receiver IDs from ISA")
        void shouldExtractSenderAndReceiver() {
            X12Interchange interchange = parser.parse(Target850Processor.SAMPLE_TARGET_850);

            assertThat(interchange.getSenderId()).isEqualTo("TARGET");
            assertThat(interchange.getReceiverId()).isEqualTo("VENDORABC");
        }

        @Test
        @DisplayName("should extract interchange control number from ISA13")
        void shouldExtractControlNumber() {
            X12Interchange interchange = parser.parse(Target850Processor.SAMPLE_TARGET_850);

            assertThat(interchange.getControlNumber()).isEqualTo("000000042");
        }

        @Test
        @DisplayName("should detect correct element delimiter as star (*)")
        void shouldDetectElementDelimiter() {
            X12Interchange interchange = parser.parse(Target850Processor.SAMPLE_TARGET_850);

            assertThat(interchange.getElementDelimiter()).isEqualTo('*');
        }

        @Test
        @DisplayName("should detect correct segment terminator as tilde (~)")
        void shouldDetectSegmentTerminator() {
            X12Interchange interchange = parser.parse(Target850Processor.SAMPLE_TARGET_850);

            assertThat(interchange.getSegmentTerminator()).isEqualTo('~');
        }
    }

    @Nested
    @DisplayName("GS / ST Envelope Parsing")
    class EnvelopeParsing {

        @Test
        @DisplayName("should parse exactly one functional group")
        void shouldParseOneGroup() {
            X12Interchange interchange = parser.parse(Target850Processor.SAMPLE_TARGET_850);

            assertThat(interchange.getGroups()).hasSize(1);
        }

        @Test
        @DisplayName("should parse exactly one 850 transaction inside the group")
        void shouldParseOneTransaction() {
            X12Interchange interchange = parser.parse(Target850Processor.SAMPLE_TARGET_850);
            var transactions = interchange.getGroups().get(0).getTransactions();

            assertThat(transactions).hasSize(1);
            assertThat(transactions.get(0).getTransactionSetCode()).isEqualTo("850");
        }

        @Test
        @DisplayName("should capture the transaction control number from ST02")
        void shouldCaptureTransactionControlNumber() {
            X12Transaction transaction = getFirstTransaction();

            assertThat(transaction.getControlNumber()).isEqualTo("0001");
        }
    }

    @Nested
    @DisplayName("Segment Extraction")
    class SegmentExtraction {

        @Test
        @DisplayName("should find BEG segment and extract PO number from position 3")
        void shouldFindBegAndExtractPoNumber() {
            X12Transaction transaction = getFirstTransaction();
            X12Segment beg = transaction.findFirst("BEG");

            assertThat(beg).isNotNull();
            assertThat(beg.getElement(3)).isEqualTo("TGT-2026-00042");
        }

        @Test
        @DisplayName("should find BEG segment and extract PO date from position 5")
        void shouldExtractPoDate() {
            X12Transaction transaction = getFirstTransaction();
            X12Segment beg = transaction.findFirst("BEG");

            assertThat(beg.getElement(5)).isEqualTo("20260219");
        }

        @Test
        @DisplayName("should extract ship-to name from N1 segment at position 2")
        void shouldExtractShipToName() {
            X12Transaction transaction = getFirstTransaction();
            X12Segment n1 = transaction.findFirst("N1");

            assertThat(n1.getElement(1)).isEqualTo("ST");
            assertThat(n1.getElement(2)).isEqualTo("Target Store #1742");
        }

        @Test
        @DisplayName("should find exactly 2 PO1 line item segments")
        void shouldFindTwoPO1Segments() {
            X12Transaction transaction = getFirstTransaction();
            List<X12Segment> po1Segments = transaction.findAll("PO1");

            assertThat(po1Segments).hasSize(2);
        }

        @Test
        @DisplayName("should extract correct SKU from first PO1 at position 7")
        void shouldExtractFirstLineSku() {
            X12Transaction transaction = getFirstTransaction();
            X12Segment firstPo1 = transaction.findAll("PO1").get(0);

            assertThat(firstPo1.getElement(2)).isEqualTo("120");    // quantity
            assertThat(firstPo1.getElement(4)).isEqualTo("24.99");  // price
            assertThat(firstPo1.getElement(7)).isEqualTo("089541234567"); // SKU
        }

        @Test
        @DisplayName("should extract correct SKU from second PO1 at position 7")
        void shouldExtractSecondLineSku() {
            X12Transaction transaction = getFirstTransaction();
            X12Segment secondPo1 = transaction.findAll("PO1").get(1);

            assertThat(secondPo1.getElement(2)).isEqualTo("60");
            assertThat(secondPo1.getElement(4)).isEqualTo("49.99");
            assertThat(secondPo1.getElement(7)).isEqualTo("089599876543");
        }

        @Test
        @DisplayName("should return empty string for out-of-range element position")
        void shouldReturnEmptyForOutOfRangePosition() {
            X12Transaction transaction = getFirstTransaction();
            X12Segment beg = transaction.findFirst("BEG");

            assertThat(beg.getElement(99)).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Failure Cases")
    class FailureCases {

        @Test
        @DisplayName("should throw EdiParseException for null content")
        void shouldThrowForNullContent() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(EdiParseException.class)
                    .hasMessageContaining("empty or null");
        }

        @Test
        @DisplayName("should throw EdiParseException for blank content")
        void shouldThrowForBlankContent() {
            assertThatThrownBy(() -> parser.parse("   "))
                    .isInstanceOf(EdiParseException.class)
                    .hasMessageContaining("empty or null");
        }

        @Test
        @DisplayName("should throw EdiParseException when content is too short for ISA")
        void shouldThrowForTooShortContent() {
            assertThatThrownBy(() -> parser.parse("ISA*too-short"))
                    .isInstanceOf(EdiParseException.class)
                    .hasMessageContaining("too short");
        }
    }

    @Nested
    @DisplayName("X12Segment element access")
    class X12SegmentElementAccess {

        @Test
        @DisplayName("should parse segment ID and elements correctly")
        void shouldParseSegmentCorrectly() {
            X12Segment seg = new X12Segment("BEG*00*SA*TGT-2026-00042**20260219", '*', 1);

            assertThat(seg.getSegmentId()).isEqualTo("BEG");
            assertThat(seg.getElement(1)).isEqualTo("00");
            assertThat(seg.getElement(2)).isEqualTo("SA");
            assertThat(seg.getElement(3)).isEqualTo("TGT-2026-00042");
            assertThat(seg.getElement(4)).isEqualTo("");   // empty element
            assertThat(seg.getElement(5)).isEqualTo("20260219");
        }

        @Test
        @DisplayName("qualifiedRef should return segment ID + zero-padded position")
        void shouldBuildQualifiedRef() {
            X12Segment seg = new X12Segment("BEG*00*SA*TGT-001", '*', 1);

            assertThat(seg.qualifiedRef(3)).isEqualTo("BEG03");
            assertThat(seg.qualifiedRef(10)).isEqualTo("BEG10");
        }
    }

    private X12Transaction getFirstTransaction() {
        X12Interchange interchange = parser.parse(Target850Processor.SAMPLE_TARGET_850);
        return interchange.getGroups().get(0).getTransactions().get(0);
    }
}
