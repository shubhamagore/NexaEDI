package com.nexaedi.portal.service;

import com.nexaedi.portal.model.*;
import com.nexaedi.portal.repository.SellerOrderRepository;
import com.nexaedi.portal.repository.SellerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Seeds the local H2 database with realistic test data for 3 seller tenants.
 *
 *  Seller 1: Dave Mitchell    — Shopify, Target + Walmart
 *  Seller 2: Maria Garcia     — WooCommerce, Walmart + Kroger
 *  Seller 3: Chen Wei         — Amazon Seller Central, Costco + Target
 *
 * Only active when the "local" Spring profile is running.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class TestDataSeeder {

    private final SellerRepository sellerRepository;
    private final SellerOrderRepository orderRepository;

    @PostConstruct
    @Transactional
    public void seed() {
        if (sellerRepository.count() > 0) {
            log.info("[SEEDER] Test data already exists, skipping.");
            return;
        }

        log.info("[SEEDER] Seeding test data for 3 seller tenants...");

        seedDave();
        seedMaria();
        seedChen();

        log.info("[SEEDER] ✓ Seeded {} sellers and {} orders.",
                sellerRepository.count(), orderRepository.count());
    }

    // ── Dave Mitchell — Shopify seller, Target + Walmart ─────────────────────
    private void seedDave() {
        Seller dave = Seller.builder()
                .name("Dave Mitchell")
                .email("dave@mitchellgadgets.com")
                .companyName("Mitchell Gadgets LLC")
                .plan(SellerPlan.GROWTH)
                .createdAt(Instant.now().minus(90, ChronoUnit.DAYS))
                .monthlyOrderCount(11)
                .build();

        ConnectedPlatform shopify = ConnectedPlatform.builder()
                .seller(dave)
                .platformType(PlatformType.SHOPIFY)
                .platformName("Mitchell Gadgets Store")
                .platformUrl("mitchellgadgets.myshopify.com")
                .status("CONNECTED")
                .connectedAt(Instant.now().minus(88, ChronoUnit.DAYS))
                .ordersSynced(18)
                .build();

        RetailerConnection target = RetailerConnection.builder()
                .seller(dave)
                .retailerId("TARGET")
                .retailerDisplayName("Target Corporation")
                .ingestionMethod("SFTP")
                .sftpPath("/nexaedi/dave-mitchell/target/inbound/")
                .status("ACTIVE")
                .connectedSince(Instant.now().minus(85, ChronoUnit.DAYS))
                .lastOrderReceivedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .totalOrdersReceived(14)
                .build();

        RetailerConnection walmart = RetailerConnection.builder()
                .seller(dave)
                .retailerId("WALMART")
                .retailerDisplayName("Walmart Inc.")
                .ingestionMethod("SFTP")
                .sftpPath("/nexaedi/dave-mitchell/walmart/inbound/")
                .status("ACTIVE")
                .connectedSince(Instant.now().minus(60, ChronoUnit.DAYS))
                .lastOrderReceivedAt(Instant.now().minus(3, ChronoUnit.HOURS))
                .totalOrdersReceived(7)
                .build();

        dave.getPlatforms().add(shopify);
        dave.getRetailers().add(target);
        dave.getRetailers().add(walmart);
        sellerRepository.save(dave);

        String lineItems2 = lineItems(
                "Wireless Headphones", "WH-BT-5000", 120, "24.99",
                "Smart Watch Band", "SWB-42MM", 60, "49.99"
        );
        String lineItems3 = lineItems(
                "Bluetooth Speaker", "BTS-MINI", 80, "79.99"
        );
        String lineItems1 = lineItems(
                "Phone Case Kit", "PCK-UNIV", 200, "12.99",
                "Wireless Headphones", "WH-BT-5000", 50, "24.99"
        );

        orderRepository.saveAll(List.of(
                order(dave, "TARGET", "Target Corporation", "TGT-2026-00042",
                        PlatformType.SHOPIFY, "SH-1001", OrderSyncStatus.SYNCED,
                        "3,247.60", 2, 180, "Target Store #1742", "Minneapolis", "MN",
                        "2026-03-05", lineItems2, hoursAgo(1)),

                order(dave, "TARGET", "Target Corporation", "TGT-2026-00039",
                        PlatformType.SHOPIFY, "SH-998", OrderSyncStatus.ACKNOWLEDGED,
                        "6,399.20", 1, 80, "Target DC #401", "Chicago", "IL",
                        "2026-02-28", lineItems3, daysAgo(2)),

                order(dave, "TARGET", "Target Corporation", "TGT-2026-00035",
                        PlatformType.SHOPIFY, "SH-991", OrderSyncStatus.ACKNOWLEDGED,
                        "3,845.50", 2, 250, "Target Store #0892", "Dallas", "TX",
                        "2026-02-20", lineItems1, daysAgo(5)),

                order(dave, "TARGET", "Target Corporation", "TGT-2026-00028",
                        PlatformType.SHOPIFY, null, OrderSyncStatus.FAILED,
                        "1,234.00", 1, 100, "Target Store #1105", "Seattle", "WA",
                        "2026-02-15", lineItems(
                                "Unknown SKU Item", "UNKNOWN-SKU-001", 100, "12.34"),
                        daysAgo(7)),

                order(dave, "WALMART", "Walmart Inc.", "WMT-2026-00099",
                        PlatformType.SHOPIFY, "SH-987", OrderSyncStatus.SYNCED,
                        "8,799.00", 1, 110, "Walmart DC #6042", "Bentonville", "AR",
                        "2026-03-10", lineItems(
                                "Bluetooth Speaker", "BTS-MINI", 110, "79.99"),
                        hoursAgo(3)),

                order(dave, "WALMART", "Walmart Inc.", "WMT-2026-00091",
                        PlatformType.SHOPIFY, "SH-980", OrderSyncStatus.ACKNOWLEDGED,
                        "2,997.00", 2, 150, "Walmart Store #4201", "Houston", "TX",
                        "2026-03-01", lineItems(
                                "Phone Case Kit", "PCK-UNIV", 150, "12.99",
                                "Smart Watch Band", "SWB-42MM", 0, "0.00"),
                        daysAgo(10)),

                order(dave, "TARGET", "Target Corporation", "TGT-2026-00021",
                        PlatformType.SHOPIFY, "SH-972", OrderSyncStatus.ACKNOWLEDGED,
                        "4,498.00", 1, 180, "Target Store #2201", "Phoenix", "AZ",
                        "2026-02-10", lineItems(
                                "Smart Watch Band", "SWB-42MM", 180, "24.99"),
                        daysAgo(15))
        ));
    }

    // ── Maria Garcia — WooCommerce seller, Walmart + Kroger ──────────────────
    private void seedMaria() {
        Seller maria = Seller.builder()
                .name("Maria Garcia")
                .email("maria@organiclifeco.com")
                .companyName("Organic Life Co.")
                .plan(SellerPlan.STARTER)
                .createdAt(Instant.now().minus(45, ChronoUnit.DAYS))
                .monthlyOrderCount(5)
                .build();

        ConnectedPlatform woo = ConnectedPlatform.builder()
                .seller(maria)
                .platformType(PlatformType.WOOCOMMERCE)
                .platformName("Organic Life Co. Store")
                .platformUrl("organiclifeco.com/shop")
                .status("CONNECTED")
                .connectedAt(Instant.now().minus(44, ChronoUnit.DAYS))
                .ordersSynced(9)
                .build();

        RetailerConnection walmart = RetailerConnection.builder()
                .seller(maria)
                .retailerId("WALMART")
                .retailerDisplayName("Walmart Inc.")
                .ingestionMethod("SFTP")
                .sftpPath("/nexaedi/maria-garcia/walmart/inbound/")
                .status("ACTIVE")
                .connectedSince(Instant.now().minus(42, ChronoUnit.DAYS))
                .lastOrderReceivedAt(hoursAgo(6))
                .totalOrdersReceived(7)
                .build();

        RetailerConnection kroger = RetailerConnection.builder()
                .seller(maria)
                .retailerId("KROGER")
                .retailerDisplayName("Kroger Co.")
                .ingestionMethod("SFTP")
                .sftpPath("/nexaedi/maria-garcia/kroger/inbound/")
                .status("ACTIVE")
                .connectedSince(Instant.now().minus(20, ChronoUnit.DAYS))
                .lastOrderReceivedAt(daysAgo(1))
                .totalOrdersReceived(2)
                .build();

        maria.getPlatforms().add(woo);
        maria.getRetailers().add(walmart);
        maria.getRetailers().add(kroger);
        sellerRepository.save(maria);

        String organicItems = lineItems(
                "Organic Almond Butter 16oz", "OAB-16OZ", 240, "8.99",
                "Organic Chia Seeds 1lb", "OCS-1LB", 180, "6.49"
        );

        orderRepository.saveAll(List.of(
                order(maria, "WALMART", "Walmart Inc.", "WMT-2026-00204",
                        PlatformType.WOOCOMMERCE, "WOO-4451", OrderSyncStatus.SYNCED,
                        "3,320.40", 2, 420, "Walmart DC #8801", "Memphis", "TN",
                        "2026-03-08", organicItems, hoursAgo(6)),

                order(maria, "WALMART", "Walmart Inc.", "WMT-2026-00198",
                        PlatformType.WOOCOMMERCE, "WOO-4440", OrderSyncStatus.ACKNOWLEDGED,
                        "2,157.60", 2, 240, "Walmart Store #5512", "Atlanta", "GA",
                        "2026-03-01", organicItems, daysAgo(3)),

                order(maria, "KROGER", "Kroger Co.", "KRG-2026-00055",
                        PlatformType.WOOCOMMERCE, "WOO-4428", OrderSyncStatus.SYNCED,
                        "1,617.60", 2, 180, "Kroger DC #14", "Cincinnati", "OH",
                        "2026-03-12", organicItems, daysAgo(1)),

                order(maria, "WALMART", "Walmart Inc.", "WMT-2026-00183",
                        PlatformType.WOOCOMMERCE, "WOO-4412", OrderSyncStatus.ACKNOWLEDGED,
                        "4,315.20", 2, 480, "Walmart Store #3301", "Miami", "FL",
                        "2026-02-22", organicItems, daysAgo(9)),

                order(maria, "WALMART", "Walmart Inc.", "WMT-2026-00171",
                        PlatformType.WOOCOMMERCE, null, OrderSyncStatus.PROCESSING,
                        "2,697.60", 2, 300, "Walmart DC #7702", "Denver", "CO",
                        "2026-03-15", organicItems, hoursAgo(2))
        ));
    }

    // ── Chen Wei — Amazon Seller, Costco + Target ─────────────────────────────
    private void seedChen() {
        Seller chen = Seller.builder()
                .name("Chen Wei")
                .email("chen@techgearusa.com")
                .companyName("TechGear USA Inc.")
                .plan(SellerPlan.PRO)
                .createdAt(Instant.now().minus(180, ChronoUnit.DAYS))
                .monthlyOrderCount(22)
                .build();

        ConnectedPlatform amazon = ConnectedPlatform.builder()
                .seller(chen)
                .platformType(PlatformType.AMAZON_SELLER)
                .platformName("TechGear USA - Amazon")
                .platformUrl("sellercentral.amazon.com/sp?ref=sg_techgear")
                .status("CONNECTED")
                .connectedAt(Instant.now().minus(178, ChronoUnit.DAYS))
                .ordersSynced(47)
                .build();

        ConnectedPlatform shopify = ConnectedPlatform.builder()
                .seller(chen)
                .platformType(PlatformType.SHOPIFY)
                .platformName("TechGear Direct")
                .platformUrl("techgearusa.myshopify.com")
                .status("CONNECTED")
                .connectedAt(Instant.now().minus(90, ChronoUnit.DAYS))
                .ordersSynced(12)
                .build();

        RetailerConnection costco = RetailerConnection.builder()
                .seller(chen)
                .retailerId("COSTCO")
                .retailerDisplayName("Costco Wholesale")
                .ingestionMethod("AS2")
                .sftpPath("as2://nexaedi-chen-costco")
                .status("ACTIVE")
                .connectedSince(Instant.now().minus(170, ChronoUnit.DAYS))
                .lastOrderReceivedAt(hoursAgo(4))
                .totalOrdersReceived(31)
                .build();

        RetailerConnection target = RetailerConnection.builder()
                .seller(chen)
                .retailerId("TARGET")
                .retailerDisplayName("Target Corporation")
                .ingestionMethod("SFTP")
                .sftpPath("/nexaedi/chen-wei/target/inbound/")
                .status("ACTIVE")
                .connectedSince(Instant.now().minus(60, ChronoUnit.DAYS))
                .lastOrderReceivedAt(daysAgo(1))
                .totalOrdersReceived(10)
                .build();

        chen.getPlatforms().add(amazon);
        chen.getPlatforms().add(shopify);
        chen.getRetailers().add(costco);
        chen.getRetailers().add(target);
        sellerRepository.save(chen);

        String techItems = lineItems(
                "4K Webcam Pro", "WC-4K-PRO", 500, "89.99",
                "USB-C Hub 7-Port", "USBCH-7P", 300, "39.99"
        );

        orderRepository.saveAll(List.of(
                order(chen, "COSTCO", "Costco Wholesale", "CST-2026-01142",
                        PlatformType.AMAZON_SELLER, "AMZ-B09XK", OrderSyncStatus.ACKNOWLEDGED,
                        "56,995.00", 2, 800, "Costco DC #801", "Seattle", "WA",
                        "2026-03-20", techItems, hoursAgo(4)),

                order(chen, "COSTCO", "Costco Wholesale", "CST-2026-01138",
                        PlatformType.AMAZON_SELLER, "AMZ-B09XH", OrderSyncStatus.SYNCED,
                        "44,995.00", 2, 650, "Costco DC #402", "Issaquah", "WA",
                        "2026-03-15", techItems, daysAgo(2)),

                order(chen, "TARGET", "Target Corporation", "TGT-2026-00088",
                        PlatformType.SHOPIFY, "SH-2001", OrderSyncStatus.SYNCED,
                        "26,997.00", 1, 300, "Target DC #601", "Brooklyn Park", "MN",
                        "2026-03-18", lineItems(
                                "4K Webcam Pro", "WC-4K-PRO", 300, "89.99"),
                        daysAgo(1)),

                order(chen, "COSTCO", "Costco Wholesale", "CST-2026-01121",
                        PlatformType.AMAZON_SELLER, "AMZ-B09XA", OrderSyncStatus.ACKNOWLEDGED,
                        "35,996.00", 2, 500, "Costco DC #205", "San Jose", "CA",
                        "2026-03-05", techItems, daysAgo(8)),

                order(chen, "TARGET", "Target Corporation", "TGT-2026-00075",
                        PlatformType.SHOPIFY, "SH-1998", OrderSyncStatus.ACKNOWLEDGED,
                        "11,998.50", 2, 300, "Target Store #4412", "Los Angeles", "CA",
                        "2026-03-01", techItems, daysAgo(11)),

                order(chen, "COSTCO", "Costco Wholesale", "CST-2026-01108",
                        PlatformType.AMAZON_SELLER, null, OrderSyncStatus.FAILED,
                        "23,997.00", 1, 300, "Costco DC #108", "Atlanta", "GA",
                        "2026-02-28", lineItems(
                                "4K Webcam Pro", "WC-4K-PRO", 300, "79.99"),
                        daysAgo(14))
        ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SellerOrder order(Seller seller, String retailerId, String retailerDisplayName,
                              String poNumber, PlatformType platform, String platformOrderId,
                              OrderSyncStatus status, String value, int lineItemCount,
                              int totalUnits, String shipToName, String city, String state,
                              String deliveryDate, String lineItemsJson, Instant receivedAt) {
        return SellerOrder.builder()
                .seller(seller)
                .retailerId(retailerId)
                .retailerDisplayName(retailerDisplayName)
                .poNumber(poNumber)
                .platformType(platform)
                .platformOrderId(platformOrderId)
                .status(status)
                .orderValue(new BigDecimal(value.replace(",", "")))
                .currency("USD")
                .lineItemCount(lineItemCount)
                .totalUnits(totalUnits)
                .shipToName(shipToName)
                .shipToCity(city)
                .shipToState(state)
                .requestedDeliveryDate(deliveryDate)
                .lineItemsJson(lineItemsJson)
                .correlationId(UUID.randomUUID().toString())
                .receivedAt(receivedAt)
                .syncedAt(status == OrderSyncStatus.SYNCED || status == OrderSyncStatus.ACKNOWLEDGED
                        ? receivedAt.plusSeconds(28) : null)
                .errorMessage(status == OrderSyncStatus.FAILED
                        ? "SKU validation failed: product not found in platform catalog" : null)
                .build();
    }

    private String lineItems(Object... parts) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < parts.length; i += 4) {
            if (i > 0) sb.append(",");
            String description = String.valueOf(parts[i]);
            String sku         = String.valueOf(parts[i + 1]);
            int    quantity    = parts[i + 2] instanceof Integer q ? q : Integer.parseInt(String.valueOf(parts[i + 2]));
            double unitPrice   = parts[i + 3] instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(parts[i + 3]));
            sb.append(String.format(
                    "{\"description\":\"%s\",\"sku\":\"%s\",\"quantity\":%d,\"unitPrice\":%.2f,\"lineTotal\":%.2f}",
                    description, sku, quantity, unitPrice, quantity * unitPrice
            ));
        }
        sb.append("]");
        return sb.toString();
    }

    private Instant daysAgo(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    private Instant hoursAgo(int hours) {
        return Instant.now().minus(hours, ChronoUnit.HOURS);
    }
}
