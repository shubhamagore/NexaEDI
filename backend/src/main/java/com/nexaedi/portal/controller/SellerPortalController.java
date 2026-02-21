package com.nexaedi.portal.controller;

import com.nexaedi.portal.model.*;
import com.nexaedi.portal.repository.SellerOrderRepository;
import com.nexaedi.portal.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for the customer-facing seller portal.
 * Returns plain business data — no EDI jargon visible to the seller.
 */
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerPortalController {

    private final SellerRepository sellerRepository;
    private final SellerOrderRepository orderRepository;

    // ── Sellers ──────────────────────────────────────────────────────────────

    @GetMapping("/sellers")
    public ResponseEntity<List<Map<String, Object>>> listSellers() {
        List<Map<String, Object>> result = sellerRepository.findAll().stream()
                .map(s -> m(
                        "id",        s.getId(),
                        "name",      s.getName(),
                        "email",     s.getEmail(),
                        "company",   s.getCompanyName(),
                        "plan",      s.getPlan().name(),
                        "planPrice", s.getPlan().getMonthlyPriceUsd()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sellers/{sellerId}")
    public ResponseEntity<Map<String, Object>> getSeller(@PathVariable Long sellerId) {
        return sellerRepository.findById(sellerId)
                .map(s -> {
                    Map<String, Object> body = m(
                            "id",        s.getId(),
                            "name",      s.getName(),
                            "email",     s.getEmail(),
                            "company",   s.getCompanyName(),
                            "plan",      s.getPlan().name(),
                            "planPrice", s.getPlan().getMonthlyPriceUsd()
                    );
                    body.put("platforms", s.getPlatforms().stream().map(this::mapPlatform).collect(Collectors.toList()));
                    body.put("retailers", s.getRetailers().stream().map(this::mapRetailer).collect(Collectors.toList()));
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/sellers/{sellerId}/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@PathVariable Long sellerId) {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant sevenDaysAgo  = Instant.now().minus(7, ChronoUnit.DAYS);

        BigDecimal revenueThisMonth = orderRepository.sumOrderValueSince(sellerId, thirtyDaysAgo);
        BigDecimal revenueThisWeek  = orderRepository.sumOrderValueSince(sellerId, sevenDaysAgo);
        long ordersThisMonth        = orderRepository.countOrdersSince(sellerId, thirtyDaysAgo);
        long totalOrders            = orderRepository.countBySellerId(sellerId);
        long pendingOrders          = orderRepository.countBySellerIdAndStatus(sellerId, OrderSyncStatus.RECEIVED)
                                    + orderRepository.countBySellerIdAndStatus(sellerId, OrderSyncStatus.PROCESSING);
        long failedOrders           = orderRepository.countBySellerIdAndStatus(sellerId, OrderSyncStatus.FAILED);
        long syncedOrders           = orderRepository.countBySellerIdAndStatus(sellerId, OrderSyncStatus.SYNCED)
                                    + orderRepository.countBySellerIdAndStatus(sellerId, OrderSyncStatus.ACKNOWLEDGED);

        List<SellerOrder> recentOrders = orderRepository.findBySellerIdOrderByReceivedAtDesc(
                sellerId, PageRequest.of(0, 5));

        Seller seller = sellerRepository.findById(sellerId).orElseThrow();
        List<Map<String, Object>> revenueByRetailer = seller.getRetailers().stream()
                .map(r -> m(
                        "retailerId",   r.getRetailerId(),
                        "retailerName", r.getRetailerDisplayName(),
                        "revenue",      orderRepository.sumOrderValueByRetailerSince(sellerId, r.getRetailerId(), thirtyDaysAgo),
                        "lastOrderAt",  r.getLastOrderReceivedAt() != null ? r.getLastOrderReceivedAt().toString() : ""
                ))
                .collect(Collectors.toList());

        double successRate = totalOrders > 0 ? (double) syncedOrders / totalOrders * 100 : 100.0;

        Map<String, Object> body = m(
                "revenueThisMonth", revenueThisMonth,
                "revenueThisWeek",  revenueThisWeek,
                "ordersThisMonth",  ordersThisMonth,
                "totalOrders",      totalOrders,
                "pendingOrders",    pendingOrders,
                "failedOrders",     failedOrders,
                "syncedOrders",     syncedOrders,
                "successRate",      Math.round(successRate)
        );
        body.put("revenueByRetailer", revenueByRetailer);
        body.put("recentOrders", recentOrders.stream().map(this::mapOrder).collect(Collectors.toList()));

        return ResponseEntity.ok(body);
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    @GetMapping("/sellers/{sellerId}/orders")
    public ResponseEntity<List<Map<String, Object>>> getOrders(
            @PathVariable Long sellerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String retailerId) {

        List<SellerOrder> orders;
        if (retailerId != null && !retailerId.isBlank()) {
            orders = orderRepository.findBySellerIdAndRetailerIdOrderByReceivedAtDesc(sellerId, retailerId);
        } else if (status != null && !status.isBlank()) {
            orders = orderRepository.findBySellerIdAndStatusOrderByReceivedAtDesc(
                    sellerId, OrderSyncStatus.valueOf(status.toUpperCase()));
        } else {
            orders = orderRepository.findBySellerIdOrderByReceivedAtDesc(sellerId);
        }

        return ResponseEntity.ok(orders.stream().map(this::mapOrder).collect(Collectors.toList()));
    }

    @GetMapping("/sellers/{sellerId}/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetail(
            @PathVariable Long sellerId,
            @PathVariable Long orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getSeller().getId().equals(sellerId))
                .map(o -> ResponseEntity.ok(mapOrderDetail(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Platforms & Retailers ────────────────────────────────────────────────

    @GetMapping("/sellers/{sellerId}/platforms")
    public ResponseEntity<List<Map<String, Object>>> getPlatforms(@PathVariable Long sellerId) {
        return sellerRepository.findById(sellerId)
                .map(s -> ResponseEntity.ok(
                        s.getPlatforms().stream().map(this::mapPlatform).collect(Collectors.toList())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sellers/{sellerId}/retailers")
    public ResponseEntity<List<Map<String, Object>>> getRetailers(@PathVariable Long sellerId) {
        return sellerRepository.findById(sellerId)
                .map(s -> ResponseEntity.ok(
                        s.getRetailers().stream().map(this::mapRetailer).collect(Collectors.toList())))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Private mappers ───────────────────────────────────────────────────────

    private Map<String, Object> mapOrder(SellerOrder o) {
        return m(
                "id",             o.getId(),
                "poNumber",       o.getPoNumber(),
                "retailerId",     o.getRetailerId(),
                "retailerName",   o.getRetailerDisplayName(),
                "platform",       o.getPlatformType().name(),
                "platformOrderId",o.getPlatformOrderId() != null ? o.getPlatformOrderId() : "",
                "status",         o.getStatus().name(),
                "orderValue",     o.getOrderValue(),
                "currency",       o.getCurrency(),
                "lineItemCount",  o.getLineItemCount(),
                "receivedAt",     o.getReceivedAt().toString()
        );
    }

    private Map<String, Object> mapOrderDetail(SellerOrder o) {
        Map<String, Object> map = mapOrder(o);
        map.put("totalUnits",            o.getTotalUnits());
        map.put("shipToName",            nvl(o.getShipToName()));
        map.put("shipToCity",            nvl(o.getShipToCity()));
        map.put("shipToState",           nvl(o.getShipToState()));
        map.put("requestedDeliveryDate", nvl(o.getRequestedDeliveryDate()));
        map.put("lineItems",             o.getLineItemsJson() != null ? o.getLineItemsJson() : "[]");
        map.put("correlationId",         nvl(o.getCorrelationId()));
        map.put("syncedAt",              o.getSyncedAt() != null ? o.getSyncedAt().toString() : "");
        map.put("errorMessage",          nvl(o.getErrorMessage()));
        return map;
    }

    private Map<String, Object> mapPlatform(ConnectedPlatform p) {
        return m(
                "id",           p.getId(),
                "platformType", p.getPlatformType().name(),
                "platformName", p.getPlatformName(),
                "platformUrl",  nvl(p.getPlatformUrl()),
                "status",       p.getStatus(),
                "connectedAt",  p.getConnectedAt().toString(),
                "ordersSynced", p.getOrdersSynced()
        );
    }

    private Map<String, Object> mapRetailer(RetailerConnection r) {
        return m(
                "id",                  r.getId(),
                "retailerId",          r.getRetailerId(),
                "retailerName",        r.getRetailerDisplayName(),
                "ingestionMethod",     r.getIngestionMethod(),
                "status",              r.getStatus(),
                "connectedSince",      r.getConnectedSince().toString(),
                "lastOrderReceivedAt", r.getLastOrderReceivedAt() != null ? r.getLastOrderReceivedAt().toString() : "",
                "totalOrdersReceived", r.getTotalOrdersReceived()
        );
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Type-safe Map<String, Object> builder — avoids Map.of() inference failures
     * when mixing value types like Long, BigDecimal, int, String in the same map.
     */
    private static Map<String, Object> m(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
