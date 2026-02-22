package com.nexaedi.core.mapping;

import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MappingRegistry {

    private final ObjectMapper objectMapper;

    private final Map<String, MappingProfile> profiles = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadMappings() {
        try {
            PathMatchingResourcePatternResolver resolver =
                    new PathMatchingResourcePatternResolver();

            Resource[] resources =
                    resolver.getResources("classpath:/mappings/*.json");

            if (resources.length == 0) {
                log.warn("No mapping profiles found under classpath:/mappings/");
                return;
            }

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {

                    MappingProfile profile =
                            objectMapper.readValue(is, MappingProfile.class);

                    String key = buildKey(
                            profile.getRetailerId(),
                            profile.getTransactionSetCode()
                    );

                    profiles.put(key, profile);

                    log.info("Loaded mapping profile: {} v{}",
                            key, profile.getVersion());

                } catch (Exception e) {
                    log.error("Failed loading mapping from {}: {}",
                            resource.getFilename(), e.getMessage(), e);
                }
            }

            log.info("MappingRegistry initialized with {} profile(s)",
                    profiles.size());

        } catch (Exception e) {
            log.error("Failed to scan classpath mappings directory", e);
        }
    }

    public Optional<MappingProfile> find(String retailerId,
                                         String transactionSetCode) {
        return Optional.ofNullable(
                profiles.get(buildKey(retailerId, transactionSetCode))
        );
    }

    public Map<String, MappingProfile> getAllProfiles() {
        return Map.copyOf(profiles);
    }

    private String buildKey(String retailerId,
                            String transactionSetCode) {
        return retailerId.toUpperCase() + ":" + transactionSetCode;
    }
}