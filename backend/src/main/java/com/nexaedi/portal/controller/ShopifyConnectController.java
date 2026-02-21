package com.nexaedi.portal.controller;

import com.nexaedi.auth.service.JwtService;
import com.nexaedi.portal.model.ConnectedPlatform;
import com.nexaedi.portal.model.PlatformType;
import com.nexaedi.portal.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * Handles connecting a seller's Shopify store to NexaEDI.
 *
 * Flow (simplified for testing — no full OAuth required):
 *  1. Seller creates a Custom App in their Shopify Admin
 *  2. Gets the Admin API access token from the app
 *  3. Pastes their store domain and token into the NexaEDI portal
 *  4. We verify the token works by calling /admin/api/shop.json
 *  5. We store the token linked to their seller account
 *
 * When EDI arrives, we use this stored token to create orders in their store.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/portal/sellers/{sellerId}/shopify")
@RequiredArgsConstructor
@Transactional
public class ShopifyConnectController {

    private final SellerRepository sellerRepository;
    private final JwtService jwtService;
    private final RestClient restClient;

    /**
     * Connects a Shopify store by verifying and storing the access token.
     *
     * Body: { "storeDomain": "my-store.myshopify.com", "accessToken": "shpat_xxxxx" }
     */
    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> connectShopify(
            @PathVariable Long sellerId,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String authHeader) {

        String storeDomain  = body.get("storeDomain");
        String accessToken  = body.get("accessToken");

        if (storeDomain == null || accessToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "storeDomain and accessToken are required"));
        }

        String cleanDomain = storeDomain.trim()
                .replace("https://", "")
                .replace("http://", "")
                .replaceAll("/$", "");

        // Verify the token by calling Shopify's shop endpoint
        Map<String, Object> shopInfo;
        try {
            shopInfo = verifyShopifyToken(cleanDomain, accessToken);
        } catch (Exception e) {
            log.warn("[SHOPIFY-CONNECT] Token verification failed for {}: {}", cleanDomain, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Could not connect to Shopify. Check your store domain and access token.",
                    "detail", e.getMessage()
            ));
        }

        String shopName = extractShopName(shopInfo);

        // Save or update the ConnectedPlatform record
        var seller = sellerRepository.findById(sellerId).orElseThrow();

        // Remove existing Shopify connection if any
        seller.getPlatforms().removeIf(p -> p.getPlatformType() == PlatformType.SHOPIFY);

        ConnectedPlatform platform = ConnectedPlatform.builder()
                .seller(seller)
                .platformType(PlatformType.SHOPIFY)
                .platformName(shopName)
                .platformUrl(cleanDomain)
                .storeDomain(cleanDomain)
                .accessToken(accessToken)
                .status("CONNECTED")
                .connectedAt(Instant.now())
                .ordersSynced(0)
                .build();

        seller.getPlatforms().add(platform);
        sellerRepository.save(seller);

        log.info("[SHOPIFY-CONNECT] sellerId={} connected store={} name='{}'", sellerId, cleanDomain, shopName);

        return ResponseEntity.ok(Map.of(
                "success",     true,
                "storeDomain", cleanDomain,
                "shopName",    shopName,
                "message",     "Shopify store connected successfully! Orders from retailers will now appear in your store automatically."
        ));
    }

    /**
     * Tests an existing Shopify connection.
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long sellerId) {
        var seller = sellerRepository.findById(sellerId).orElseThrow();
        var shopifyPlatform = seller.getPlatforms().stream()
                .filter(p -> p.getPlatformType() == PlatformType.SHOPIFY)
                .findFirst();

        if (shopifyPlatform.isEmpty()) {
            return ResponseEntity.ok(Map.of("connected", false, "message", "No Shopify store connected yet."));
        }

        ConnectedPlatform p = shopifyPlatform.get();
        try {
            Map<String, Object> shopInfo = verifyShopifyToken(p.getStoreDomain(), p.getAccessToken());
            return ResponseEntity.ok(Map.of(
                    "connected",   true,
                    "storeDomain", p.getStoreDomain(),
                    "shopName",    extractShopName(shopInfo),
                    "status",      "CONNECTED"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "connected", false,
                    "message",   "Connection failed: " + e.getMessage()
            ));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyShopifyToken(String storeDomain, String accessToken) {
        String url = "https://" + storeDomain + "/admin/api/2024-01/shop.json";
        Map<String, Object> response = restClient.get()
                .uri(url)
                .header("X-Shopify-Access-Token", accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .body(Map.class);
        if (response == null || !response.containsKey("shop")) {
            throw new RuntimeException("Invalid response from Shopify");
        }
        return (Map<String, Object>) response.get("shop");
    }

    @SuppressWarnings("unchecked")
    private String extractShopName(Map<String, Object> shopInfo) {
        Object name = shopInfo.get("name");
        return name != null ? name.toString() : "My Shopify Store";
    }
}
