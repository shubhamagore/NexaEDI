package com.nexaedi.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "nexaedi.s3.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class DatabaseStorageService implements StorageService {

    @Override
    public String storeInbound(String correlationId, String retailerId, String content) {
        return "db://" + correlationId;
    }

    @Override
    public String storeOutbound(String correlationId, String retailerId, String content) {
        return "db-outbound://" + correlationId;
    }

    @Override
    public String retrieveContent(String key) {
        return "";
    }

    @Override
    public String archiveProcessed(String key, String correlationId) {
        return key;
    }
}