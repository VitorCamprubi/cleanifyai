package com.cleanifyai.api.dto.shared;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String message,
        String path) {
}

