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

/**
 * Issues and validates the JWTs used for API authentication.
 *
 * <p>The signing key comes from {@code cowtrack.jwt.secret} and must be at least
 * 32 bytes for HMAC-SHA256. Tokens carry the user id and role as claims so that
 * request authorisation does not need a database round trip.
 */
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

    public String generateToken(String email, Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claims(Map.of("userId", userId, "role", role))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(signingKey)
                .compact();
    }

    /** Returns the subject (email), or null if the token is invalid or expired. */
    public String extractEmail(String token) {
        Claims claims = parse(token);
        return claims == null ? null : claims.getSubject();
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
