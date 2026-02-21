package com.nexaedi.infrastructure.shopify;

import com.nexaedi.core.model.CanonicalOrder;
import com.nexaedi.portal.model.PlatformType;
import com.nexaedi.portal.repository.ConnectedPlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

/**
 * Adapter for transmitting canonical orders to Shopify via the Admin API.
 *
 * Token resolution priority:
 *  1. Seller's stored Shopify access token (from ConnectedPlatform) — for real sellers
 *  2. Global ShopifyProperties config — fallback for direct API testing
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopifyOutboundAdapter {

    private final ShopifyProperties properties;
    private final ShopifyRateLimiter rateLimiter;
    private final RestClient restClient;
    private final ConnectedPlatformRepository platformRepository;

    @Retryable(
        retryFor = { HttpServerErrorException.class, ShopifyTransmissionException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 30000)
    )
    public String transmit(CanonicalOrder order) {
        try {
            if (rateLimiter != null) rateLimiter.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShopifyTransmissionException("Rate limiter interrupted for PO " + order.getPoNumber(), e);
        }

        // Resolve seller's Shopify token and store domain
        ShopifyTarget target = resolveTarget(order);

        String url = "https://" + target.storeDomain() + "/admin/api/2024-01/draft_orders.json";
        ShopifyOrderRequest requestPayload = ShopifyOrderRequest.from(order);

        log.info("[SHOPIFY] Transmitting PO {} to store: {}", order.getPoNumber(), target.storeDomain());

        try {
            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .header("X-Shopify-Access-Token", target.accessToken())
                    .header("Content-Type", "application/json")
                    .body(requestPayload)
                    .retrieve()
                    .body(Map.class);

            String shopifyOrderId = extractDraftOrderId(response);
            log.info("[SHOPIFY] ✓ Draft Order created: {} for PO {}", shopifyOrderId, order.getPoNumber());
            return shopifyOrderId;

        } catch (HttpClientErrorException e) {
            log.error("[SHOPIFY] Client error ({}): {} for PO {}", e.getStatusCode(), e.getResponseBodyAsString(), order.getPoNumber());
            throw new ShopifyTransmissionException("Shopify rejected (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            log.warn("[SHOPIFY] Server error ({}), will retry PO {}", e.getStatusCode(), order.getPoNumber());
            throw e;
        }
    }

    public String fetchAccessToken() {
        return "not-used-with-per-seller-tokens";
    }

    private ShopifyTarget resolveTarget(CanonicalOrder order) {
        // Look up seller's connected Shopify platform directly (no lazy loading)
        try {
            var platforms = platformRepository.findAllByPlatformTypeWithToken(PlatformType.SHOPIFY);
            // Pick the most recently connected platform that has a real store domain (not null/blank)
            var platform = platforms.stream()
                    .filter(p -> p.getStoreDomain() != null && !p.getStoreDomain().isBlank()
                            && !p.getStoreDomain().equals("local-stub"))
                    .findFirst();
            if (platform.isPresent()) {
                var p = platform.get();
                log.info("[SHOPIFY] Using seller token for store: {}", p.getStoreDomain());
                return new ShopifyTarget(p.getStoreDomain(), p.getAccessToken());
            }
        } catch (Exception e) {
            log.warn("[SHOPIFY] Could not resolve seller token, falling back to config: {}", e.getMessage());
        }

        // Fallback to global config (only used if no seller has connected a store)
        String domain = properties.getStoreName() + ".myshopify.com";
        log.warn("[SHOPIFY] No seller Shopify token found — using fallback config store: {}", domain);
        return new ShopifyTarget(domain, properties.getClientSecret());
    }

    @SuppressWarnings("unchecked")
    private String extractDraftOrderId(Map<?, ?> response) {
        if (response == null) throw new ShopifyTransmissionException("Empty response from Shopify");
        Map<String, Object> draftOrder = (Map<String, Object>) response.get("draft_order");
        if (draftOrder == null || !draftOrder.containsKey("id")) {
            throw new ShopifyTransmissionException("Missing 'draft_order.id' in Shopify response");
        }
        return String.valueOf(draftOrder.get("id"));
    }

    private record ShopifyTarget(String storeDomain, String accessToken) {}
}
