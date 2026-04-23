package com.cleanifyai.api.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.cleanifyai.api.config.AppProperties;
import com.cleanifyai.api.domain.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final AppProperties appProperties;
    private final SecretKey signingKey;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.signingKey = buildSigningKey();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(appProperties.getSecurity().getJwt().getExpirationSeconds());

        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claims(Map.of(
                        "role", user.getRole().name(),
                        "name", user.getNome()))
                .signWith(signingKey)
                .compact();
    }

    public Instant extractExpiration(String token) {
        return extractAllClaims(token).getExpiration().toInstant();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, AuthenticatedUser authenticatedUser) {
        String username = extractUsername(token);
        return username.equalsIgnoreCase(authenticatedUser.getUsername()) && extractExpiration(token).isAfter(Instant.now());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey buildSigningKey() {
        String secret = appProperties.getSecurity().getJwt().getSecret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret deve ter no minimo 32 bytes");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
