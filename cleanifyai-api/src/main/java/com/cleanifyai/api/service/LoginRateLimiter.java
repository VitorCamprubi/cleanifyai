package com.cleanifyai.api.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cleanifyai.api.config.AppProperties;
import com.cleanifyai.api.exception.RateLimitExceededException;

@Service
public class LoginRateLimiter {

    private final AppProperties appProperties;
    private final Clock clock;
    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginRateLimiter(AppProperties appProperties) {
        this(appProperties, Clock.systemUTC());
    }

    LoginRateLimiter(AppProperties appProperties, Clock clock) {
        this.appProperties = appProperties;
        this.clock = clock;
    }

    public void checkAllowed(String email, String ipAddress) {
        int maxAttempts = maxAttempts();
        if (maxAttempts <= 0) {
            return;
        }

        String key = key(email, ipAddress);
        Instant now = Instant.now(clock);
        AttemptWindow window = attempts.get(key);
        if (window == null) {
            return;
        }

        if (isExpired(window, now)) {
            attempts.remove(key, window);
            return;
        }

        if (window.failures() >= maxAttempts) {
            throw new RateLimitExceededException("Muitas tentativas de login. Aguarde alguns minutos e tente novamente.");
        }
    }

    public void recordFailure(String email, String ipAddress) {
        int maxAttempts = maxAttempts();
        if (maxAttempts <= 0) {
            return;
        }

        String key = key(email, ipAddress);
        Instant now = Instant.now(clock);
        attempts.compute(key, (ignored, current) -> {
            if (current == null || isExpired(current, now)) {
                return new AttemptWindow(1, now);
            }
            return new AttemptWindow(current.failures() + 1, current.firstFailureAt());
        });
    }

    public void recordSuccess(String email, String ipAddress) {
        attempts.remove(key(email, ipAddress));
    }

    private boolean isExpired(AttemptWindow window, Instant now) {
        long windowSeconds = Math.max(1, appProperties.getSecurity().getRateLimit().getLoginWindowSeconds());
        return Duration.between(window.firstFailureAt(), now).getSeconds() >= windowSeconds;
    }

    private int maxAttempts() {
        return appProperties.getSecurity().getRateLimit().getLoginMaxAttempts();
    }

    private String key(String email, String ipAddress) {
        String normalizedEmail = email == null || email.isBlank()
                ? "<blank>"
                : email.trim().toLowerCase(Locale.ROOT);
        String normalizedIp = ipAddress == null || ipAddress.isBlank() ? "<unknown>" : ipAddress.trim();
        return normalizedEmail + "|" + normalizedIp;
    }

    private record AttemptWindow(int failures, Instant firstFailureAt) {
    }
}
