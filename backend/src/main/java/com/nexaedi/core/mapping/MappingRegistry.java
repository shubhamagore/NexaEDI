package com.nexaedi.core.mapping;

import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Self-loading registry that scans the /mappings directory at startup
 * and builds an in-memory index of all MappingProfiles.
 *
 * To add a new retailer: drop a JSON file (e.g., walmart-850.json) into
 * the configured mappings directory and restart the service.
 * The registry key format is: "{RETAILER_ID}:{TRANSACTION_SET_CODE}"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MappingRegistry {

    private final ObjectMapper objectMapper;

    @Value("${nexaedi.mappings.directory:src/main/resources/mappings}")
    private String mappingsDirectory;

    private final Map<String, MappingProfile> profiles = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadMappings() {
        Path mappingsPath = Path.of(mappingsDirectory);
        if (!Files.exists(mappingsPath)) {
            log.warn("Mappings directory '{}' not found. No profiles loaded.", mappingsDirectory);
            return;
        }

        try (Stream<Path> files = Files.list(mappingsPath)) {
            files.filter(p -> p.toString().endsWith(".json"))
                 .forEach(this::loadProfile);
        } catch (IOException e) {
            log.error("Failed to scan mappings directory '{}': {}", mappingsDirectory, e.getMessage(), e);
        }

        log.info("MappingRegistry initialized with {} profile(s): {}", profiles.size(), profiles.keySet());
    }

    private void loadProfile(Path jsonFile) {
        try {
            MappingProfile profile = objectMapper.readValue(jsonFile.toFile(), MappingProfile.class);
            String key = buildKey(profile.getRetailerId(), profile.getTransactionSetCode());
            profiles.put(key, profile);
            log.info("Loaded mapping profile: {} v{} from file '{}'",
                    key, profile.getVersion(), jsonFile.getFileName());
        } catch (Exception e) {
            log.error("Failed to load mapping profile from '{}': {}", jsonFile, e.getMessage(), e);
        }
    }

    /**
     * Looks up a MappingProfile by retailer ID and transaction set code.
     *
     * @param retailerId         e.g., "TARGET"
     * @param transactionSetCode e.g., "850"
     * @return Optional containing the profile if found
     */
    public Optional<MappingProfile> find(String retailerId, String transactionSetCode) {
        String key = buildKey(retailerId.toUpperCase(), transactionSetCode);
        return Optional.ofNullable(profiles.get(key));
    }

    /**
     * Returns all currently loaded profiles. Useful for health/info endpoints.
     */
    public Map<String, MappingProfile> getAllProfiles() {
        return Map.copyOf(profiles);
    }

    private String buildKey(String retailerId, String transactionSetCode) {
        return retailerId.toUpperCase() + ":" + transactionSetCode;
    }
}
