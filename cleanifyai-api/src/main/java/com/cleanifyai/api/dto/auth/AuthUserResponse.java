package com.cleanifyai.api.dto.auth;

import com.cleanifyai.api.domain.enums.UserRole;

public record AuthUserResponse(
        Long id,
        String nome,
        String email,
        UserRole role) {
}
