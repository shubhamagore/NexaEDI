package com.nexaedi.portal.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A selling platform connected by the seller (e.g., their Shopify store).
 */
@Entity
@Table(name = "connected_platforms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectedPlatform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    @ToString.Exclude
    private Seller seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false, length = 30)
    private PlatformType platformType;

    @Column(name = "platform_name", nullable = false)
    private String platformName;

    @Column(name = "platform_url")
    private String platformUrl;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "CONNECTED";

    @Column(name = "connected_at")
    @Builder.Default
    private Instant connectedAt = Instant.now();

    @Column(name = "orders_synced")
    @Builder.Default
    private int ordersSynced = 0;

    /**
     * Platform-specific access token (e.g., Shopify Admin API token).
     * Stored in plain text for local dev — encrypt with AES in production.
     */
    @Column(name = "access_token", length = 512)
    private String accessToken;

    @Column(name = "store_domain")
    private String storeDomain;
}
