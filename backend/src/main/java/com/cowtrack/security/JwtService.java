package com.cowtrack.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${cowtrack.jwt.secret}") String secret,
            @Value("${cowtrack.jwt.expiration-ms}") long expirationMillis) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "cowtrack.jwt.secret must be at least 32 characters; got " + keyBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(String email, Long userId, String role, Long farmId) {
        Date now = new Date();
        var claimsBuilder = new java.util.HashMap<String, Object>();
        claimsBuilder.put("userId", userId);
        claimsBuilder.put("role", role);
        if (farmId != null) {
            claimsBuilder.put("farmId", farmId);
        }
        return Jwts.builder()
                .subject(email)
                .claims(claimsBuilder)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        Claims claims = parse(token);
        return claims == null ? null : claims.getSubject();
    }

    public Long extractUserId(String token) {
        Claims claims = parse(token);
        return claims == null ? null : claims.get("userId", Long.class);
    }

    public String extractRole(String token) {
        Claims claims = parse(token);
        return claims == null ? null : claims.get("role", String.class);
    }

    public Long extractFarmId(String token) {
        Claims claims = parse(token);
        return claims == null ? null : claims.get("farmId", Long.class);
    }

    public boolean isValid(String token) {
        return parse(token) != null;
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
