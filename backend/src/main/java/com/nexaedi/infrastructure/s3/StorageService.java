package com.nexaedi.infrastructure.storage;

public interface StorageService {

    String storeInbound(String correlationId,
                        String retailerId,
                        String content,
                        String fileName);

    void archiveProcessed(String key);

    String retrieveContent(String key);
}