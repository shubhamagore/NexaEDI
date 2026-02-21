package com.nexaedi.infrastructure.shopify;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nexaedi.core.model.CanonicalOrder;
import com.nexaedi.core.model.CanonicalOrderLine;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Shopify Admin API 2026-01 Draft Order request payload.
 * Translated from a CanonicalOrder by the ShopifyOutboundAdapter.
 *
 * Reference: https://shopify.dev/docs/api/admin-rest/2026-01/resources/draftorder
 */
@Data
@Builder
public class ShopifyOrderRequest {

    @JsonProperty("draft_order")
    private DraftOrder draftOrder;

    @Data
    @Builder
    public static class DraftOrder {

        @JsonProperty("note")
        private String note;

        @JsonProperty("email")
        private String email;

        @JsonProperty("tags")
        private String tags;

        @JsonProperty("line_items")
        private List<LineItem> lineItems;

        @JsonProperty("shipping_address")
        private ShippingAddress shippingAddress;

        @JsonProperty("note_attributes")
        private List<Map<String, String>> noteAttributes;
    }

    @Data
    @Builder
    public static class LineItem {

        @JsonProperty("sku")
        private String sku;

        @JsonProperty("quantity")
        private int quantity;

        @JsonProperty("price")
        private String price;

        @JsonProperty("title")
        private String title;

        @JsonProperty("requires_shipping")
        private boolean requiresShipping;
    }

    @Data
    @Builder
    public static class ShippingAddress {

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("address1")
        private String address1;

        @JsonProperty("city")
        private String city;

        @JsonProperty("province_code")
        private String provinceCode;

        @JsonProperty("zip")
        private String zip;

        @JsonProperty("country_code")
        private String countryCode;
    }

    /**
     * Factory method that translates a CanonicalOrder into a Shopify DraftOrder request.
     */
    public static ShopifyOrderRequest from(CanonicalOrder order) {
        List<LineItem> lineItems = order.getLines().stream()
                .map(line -> LineItem.builder()
                        .sku(line.getSku())
                        .quantity(line.getQuantityOrdered())
                        .price(line.getUnitPrice().toPlainString())
                        .title(line.getProductDescription() != null
                                ? line.getProductDescription() : line.getSku())
                        .requiresShipping(true)
                        .build())
                .toList();

        ShippingAddress address = ShippingAddress.builder()
                .firstName(order.getShipToName())
                .address1(order.getShipToAddress())
                .city(order.getShipToCity())
                .provinceCode(order.getShipToState())
                .zip(order.getShipToZip())
                .countryCode("US")
                .build();

        DraftOrder draftOrder = DraftOrder.builder()
                .note("EDI PO# " + order.getPoNumber() + " from " + order.getRetailerId())
                .tags("edi,nexaedi," + order.getRetailerId().toLowerCase())
                .lineItems(lineItems)
                .shippingAddress(address)
                .noteAttributes(List.of(
                        Map.of("name", "edi_po_number", "value", order.getPoNumber()),
                        Map.of("name", "edi_retailer", "value", order.getRetailerId()),
                        Map.of("name", "nexaedi_correlation_id", "value", order.getCorrelationId())
                ))
                .build();

        return ShopifyOrderRequest.builder().draftOrder(draftOrder).build();
    }
}
