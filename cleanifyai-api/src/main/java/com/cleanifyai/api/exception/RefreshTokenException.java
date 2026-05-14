package com.cleanifyai.api.exception;

import org.springframework.security.core.AuthenticationException;

public class RefreshTokenException extends AuthenticationException {

    public RefreshTokenException(String message) {
        super(message);
    }
}
