package com.cleanifyai.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token e obrigatorio")
        String refreshToken) {
}
