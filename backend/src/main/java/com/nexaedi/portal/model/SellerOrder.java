package com.nexaedi.portal.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A business-level purchase order visible to the seller in their portal.
 * This is what Dave sees — NOT raw EDI audit logs.
 *
 * Maps 1-to-1 with the EDI 850 transaction but in business language.
 */
@Entity
@Table(
    name = "seller_orders",
    indexes = {
        @Index(name = "idx_seller_orders_seller_id", columnList = "seller_id"),
        @Index(name = "idx_seller_orders_status", columnList = "status"),
        @Index(name = "idx_seller_orders_received_at", columnList = "received_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    @ToString.Exclude
    private Seller seller;

    @Column(name = "retailer_id", nullable = false, length = 50)
    private String retailerId;

    @Column(name = "retailer_display_name", nullable = false)
    private String retailerDisplayName;

    @Column(name = "po_number", nullable = false, length = 100)
    private String poNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false, length = 30)
    private PlatformType platformType;

    @Column(name = "platform_order_id")
    private String platformOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderSyncStatus status = OrderSyncStatus.RECEIVED;

    @Column(name = "order_value", precision = 12, scale = 2)
    private BigDecimal orderValue;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "line_item_count")
    private int lineItemCount;

    @Column(name = "total_units")
    private int totalUnits;

    @Column(name = "ship_to_name")
    private String shipToName;

    @Column(name = "ship_to_city")
    private String shipToCity;

    @Column(name = "ship_to_state", length = 2)
    private String shipToState;

    @Column(name = "requested_delivery_date")
    private String requestedDeliveryDate;

    @Column(name = "line_items_json", columnDefinition = "TEXT")
    private String lineItemsJson;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private Instant receivedAt = Instant.now();

    @Column(name = "synced_at")
    private Instant syncedAt;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "department_number", length = 20)
    private String departmentNumber;
}
