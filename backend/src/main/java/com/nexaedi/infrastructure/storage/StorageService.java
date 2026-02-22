package com.nexaedi.infrastructure.storage;

public interface StorageService {

    String storeInbound(String correlationId, String retailerId, String content);

    String storeOutbound(String correlationId, String retailerId, String content);

    String retrieveContent(String key);

    String archiveProcessed(String key, String correlationId);
}