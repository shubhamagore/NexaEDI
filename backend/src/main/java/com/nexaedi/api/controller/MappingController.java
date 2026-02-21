package com.nexaedi.api.controller;

import com.nexaedi.core.mapping.MappingProfile;
import com.nexaedi.core.mapping.MappingRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for inspecting the Mapping Registry.
 * Allows operators to verify which retailer profiles are currently loaded
 * without needing to access the server filesystem.
 */
@RestController
@RequestMapping("/api/v1/mappings")
@RequiredArgsConstructor
public class MappingController {

    private final MappingRegistry mappingRegistry;

    /**
     * Lists all loaded mapping profiles (retailer ID + transaction code + version).
     */
    @GetMapping
    public ResponseEntity<Map<String, MappingProfile>> listProfiles() {
        return ResponseEntity.ok(mappingRegistry.getAllProfiles());
    }

    /**
     * Retrieves a specific mapping profile by retailer and transaction set.
     *
     * @param retailerId         e.g., "target" or "TARGET" (case-insensitive)
     * @param transactionSetCode e.g., "850"
     */
    @GetMapping("/{retailerId}/{transactionSetCode}")
    public ResponseEntity<MappingProfile> getProfile(
            @PathVariable String retailerId,
            @PathVariable String transactionSetCode) {
        return mappingRegistry.find(retailerId.toUpperCase(), transactionSetCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
