package com.nexaedi.core.mapping;

import com.nexaedi.core.model.CanonicalOrder;
import com.nexaedi.core.model.X12Interchange;
import com.nexaedi.core.model.X12Transaction;
import com.nexaedi.core.parser.UniversalX12Parser;
import com.nexaedi.core.processor.Target850Processor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MappingRegistry + X12ToCanonicalMapper.
 * Loads the real target-850.json from src/main/resources/mappings
 * and maps the sample Target 850 transaction.
 * No Spring context needed.
 */
@DisplayName("MappingRegistry + X12ToCanonicalMapper")
class MappingRegistryAndMapperTest {

    private UniversalX12Parser parser;
    private MappingRegistry registry;
    private X12ToCanonicalMapper mapper;

    @BeforeEach
    void setUp() {
        parser = new UniversalX12Parser();

        ObjectMapper objectMapper = new ObjectMapper();
        registry = new MappingRegistry(objectMapper);
        ReflectionTestUtils.setField(registry, "mappingsDirectory", "src/main/resources/mappings");
        registry.loadMappings();

        mapper = new X12ToCanonicalMapper();
    }

    @Nested
    @DisplayName("MappingRegistry")
    class MappingRegistryTests {

        @Test
        @DisplayName("should load target-850 profile from JSON file")
        void shouldLoadTargetProfile() {
            var profile = registry.find("TARGET", "850");

            assertThat(profile).isPresent();
            assertThat(profile.get().getRetailerId()).isEqualTo("TARGET");
            assertThat(profile.get().getTransactionSetCode()).isEqualTo("850");
        }

        @Test
        @DisplayName("should load walmart-850 profile from JSON file")
        void shouldLoadWalmartProfile() {
            var profile = registry.find("WALMART", "850");

            assertThat(profile).isPresent();
            assertThat(profile.get().getRetailerId()).isEqualTo("WALMART");
        }

        @Test
        @DisplayName("should be case-insensitive for retailer ID lookup")
        void shouldBeCaseInsensitive() {
            assertThat(registry.find("target", "850")).isPresent();
            assertThat(registry.find("Target", "850")).isPresent();
            assertThat(registry.find("TARGET", "850")).isPresent();
        }

        @Test
        @DisplayName("should return empty for unknown retailer")
        void shouldReturnEmptyForUnknownRetailer() {
            var profile = registry.find("UNKNOWNRETAILER", "850");

            assertThat(profile).isEmpty();
        }

        @Test
        @DisplayName("should load at least 2 profiles (target + walmart)")
        void shouldLoadAtLeastTwoProfiles() {
            assertThat(registry.getAllProfiles()).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("X12ToCanonicalMapper — Target 850")
    class MapperTests {

        private CanonicalOrder order;

        @BeforeEach
        void mapSampleOrder() {
            X12Interchange interchange = parser.parse(Target850Processor.SAMPLE_TARGET_850);
            X12Transaction transaction = interchange.getGroups().get(0).getTransactions().get(0);
            MappingProfile profile = registry.find("TARGET", "850").orElseThrow();

            order = mapper.map(transaction, profile, "TARGET");
        }

        @Test
        @DisplayName("should map PO number from BEG03")
        void shouldMapPoNumber() {
            assertThat(order.getPoNumber()).isEqualTo("TGT-2026-00042");
        }

        @Test
        @DisplayName("should map PO type from BEG02")
        void shouldMapPurchaseOrderType() {
            assertThat(order.getPurchaseOrderType()).isEqualTo("SA");
        }

        @Test
        @DisplayName("should map PO date from BEG05 as LocalDate")
        void shouldMapPoDate() {
            assertThat(order.getPoDate()).isEqualTo(LocalDate.of(2026, 2, 19));
        }

        @Test
        @DisplayName("should map ship-to name from N1 where N101=ST")
        void shouldMapShipToName() {
            assertThat(order.getShipToName()).isEqualTo("Target Store #1742");
        }

        @Test
        @DisplayName("should map ship-to city from N401")
        void shouldMapShipToCity() {
            assertThat(order.getShipToCity()).isEqualTo("Minneapolis");
        }

        @Test
        @DisplayName("should map ship-to state from N402")
        void shouldMapShipToState() {
            assertThat(order.getShipToState()).isEqualTo("MN");
        }

        @Test
        @DisplayName("should map ship-to ZIP from N403")
        void shouldMapShipToZip() {
            assertThat(order.getShipToZip()).isEqualTo("55402");
        }

        @Test
        @DisplayName("should map department number from REF02 where REF01=DP")
        void shouldMapDepartmentNumber() {
            assertThat(order.getDepartmentNumber()).isEqualTo("042");
        }

        @Test
        @DisplayName("should map retailer ID to TARGET")
        void shouldMapRetailerId() {
            assertThat(order.getRetailerId()).isEqualTo("TARGET");
        }

        @Test
        @DisplayName("should produce exactly 2 line items from 2 PO1 segments")
        void shouldMapTwoLineItems() {
            assertThat(order.getLines()).hasSize(2);
        }

        @Test
        @DisplayName("should map line 1 SKU, quantity, and price correctly")
        void shouldMapFirstLineItem() {
            var line1 = order.getLines().get(0);

            assertThat(line1.getSku()).isEqualTo("089541234567");
            assertThat(line1.getQuantityOrdered()).isEqualTo(120);
            assertThat(line1.getUnitPrice()).isEqualByComparingTo(new BigDecimal("24.99"));
            assertThat(line1.getUnitOfMeasure()).isEqualTo("EA");
        }

        @Test
        @DisplayName("should map line 2 SKU, quantity, and price correctly")
        void shouldMapSecondLineItem() {
            var line2 = order.getLines().get(1);

            assertThat(line2.getSku()).isEqualTo("089599876543");
            assertThat(line2.getQuantityOrdered()).isEqualTo(60);
            assertThat(line2.getUnitPrice()).isEqualByComparingTo(new BigDecimal("49.99"));
        }

        @Test
        @DisplayName("should assign sequential line sequence numbers starting at 1")
        void shouldAssignLineSequenceNumbers() {
            assertThat(order.getLines().get(0).getLineSequenceNumber()).isEqualTo(1);
            assertThat(order.getLines().get(1).getLineSequenceNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("should generate a non-blank correlation ID")
        void shouldGenerateCorrelationId() {
            assertThat(order.getCorrelationId()).isNotBlank();
        }
    }
}
