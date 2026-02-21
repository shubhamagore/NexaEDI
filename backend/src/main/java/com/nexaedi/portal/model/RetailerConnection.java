package com.nexaedi.portal.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A retail trading partner connection for a seller.
 * e.g., Dave's connection to Target via SFTP.
 */
@Entity
@Table(name = "retailer_connections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetailerConnection {

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

    @Column(name = "ingestion_method", nullable = false, length = 20)
    @Builder.Default
    private String ingestionMethod = "SFTP";

    @Column(name = "sftp_path")
    private String sftpPath;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "connected_since")
    @Builder.Default
    private Instant connectedSince = Instant.now();

    @Column(name = "last_order_received_at")
    private Instant lastOrderReceivedAt;

    @Column(name = "total_orders_received")
    @Builder.Default
    private int totalOrdersReceived = 0;
}
