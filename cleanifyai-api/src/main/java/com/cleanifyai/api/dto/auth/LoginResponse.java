package com.cleanifyai.api.dto.auth;

import java.time.Instant;

public record LoginResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        AuthUserResponse user) {
}
