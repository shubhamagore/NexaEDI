package com.nexaedi.infrastructure.storage;

public interface StorageService {
    void storeInbound(String correlationId, String retailerId, String content);
    void storeOutbound(String correlationId, String retailerId, String content);
}