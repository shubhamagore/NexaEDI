package com.nexaedi.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
public class JwtService {

    @Value("${nexaedi.auth.jwt-secret:nexaedi-super-secret-key-must-be-at-least-32-chars-long!}")
    private String jwtSecret;

    @Value("${nexaedi.auth.jwt-expiry-ms:86400000}")
    private long jwtExpiryMs;

    public String generateToken(String email, Long sellerId) {
        return Jwts.builder()
                .subject(email)
                .claims(Map.of("sellerId", sellerId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiryMs))
                .signWith(signingKey())
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractSellerId(String token) {
        Object sid = parseClaims(token).get("sellerId");
        if (sid instanceof Integer i) return i.longValue();
        if (sid instanceof Long l)    return l;
        return Long.parseLong(sid.toString());
    }

    public boolean isValid(String token, UserDetails user) {
        try {
            String email = extractEmail(token);
            return email.equals(user.getUsername()) && !isExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
