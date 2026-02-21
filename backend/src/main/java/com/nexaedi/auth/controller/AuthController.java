package com.nexaedi.auth.controller;

import com.nexaedi.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public authentication endpoints — no JWT required.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String email       = require(body, "email");
        String password    = require(body, "password");
        String fullName    = require(body, "fullName");
        String companyName = body.getOrDefault("companyName", fullName);

        if (password.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters."));
        }

        try {
            return ResponseEntity.ok(authService.register(email, password, fullName, companyName));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String email    = require(body, "email");
        String password = require(body, "password");

        try {
            return ResponseEntity.ok(authService.login(email, password));
        } catch (Exception e) {
            log.warn("[AUTH] Login failed for {}: {} — {}", email, e.getClass().getSimpleName(), e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(Map.of("status", "authenticated"));
    }

    private String require(Map<String, String> body, String key) {
        String val = body.get(key);
        if (val == null || val.isBlank()) throw new IllegalArgumentException(key + " is required");
        return val.trim();
    }
}
