package com.nexaedi.infrastructure.shopify;

/**
 * Thrown when an outbound API call to Shopify fails in a non-retryable way,
 * or when all retry attempts have been exhausted.
 */
public class ShopifyTransmissionException extends RuntimeException {

    public ShopifyTransmissionException(String message) {
        super(message);
    }

    public ShopifyTransmissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
