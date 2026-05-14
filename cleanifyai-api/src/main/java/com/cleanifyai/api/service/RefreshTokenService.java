package com.cleanifyai.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanifyai.api.config.AppProperties;
import com.cleanifyai.api.domain.entity.RefreshToken;
import com.cleanifyai.api.domain.entity.User;
import com.cleanifyai.api.exception.RefreshTokenException;
import com.cleanifyai.api.repository.RefreshTokenRepository;
import com.cleanifyai.api.repository.UserRepository;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            AppProperties appProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.appProperties = appProperties;
    }

    @Transactional
    public IssuedRefreshToken issueFor(User user, String ipAddress, String userAgent) {
        String rawToken = generateToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(appProperties.getSecurity().getRefreshToken().getExpirationSeconds());

        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setEmpresaId(user.getEmpresaId());
        token.setTokenHash(hash(rawToken));
        token.setIssuedAt(now);
        token.setExpiresAt(expiresAt);
        token.setCreatedIp(limit(ipAddress, 80));
        token.setCreatedUserAgent(limit(userAgent, 500));
        refreshTokenRepository.save(token);

        return new IssuedRefreshToken(rawToken, expiresAt, token.getTokenHash());
    }

    @Transactional
    public RotatedRefreshToken rotate(String rawToken, String ipAddress, String userAgent) {
        RefreshToken current = findValid(rawToken);
        User user = userRepository.findById(current.getUserId())
                .filter(found -> Boolean.TRUE.equals(found.getAtivo()))
                .orElseThrow(() -> new RefreshTokenException("Sessao expirada. Faca login novamente."));

        Instant now = Instant.now();
        current.setLastUsedAt(now);
        current.setRevokedAt(now);

        IssuedRefreshToken next = issueFor(user, ipAddress, userAgent);
        current.setReplacedByTokenHash(next.tokenHash());

        return new RotatedRefreshToken(user, next.token(), next.expiresAt());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String tokenHash = hash(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            Instant now = Instant.now();
            token.setLastUsedAt(now);
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
            }
        });
    }

    private RefreshToken findValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new RefreshTokenException("Sessao expirada. Faca login novamente.");
        }

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new RefreshTokenException("Sessao expirada. Faca login novamente."));

        Instant now = Instant.now();
        if (token.getRevokedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new RefreshTokenException("Sessao expirada. Faca login novamente.");
        }

        return token;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public record IssuedRefreshToken(String token, Instant expiresAt, String tokenHash) {
    }

    public record RotatedRefreshToken(User user, String token, Instant expiresAt) {
    }
}
