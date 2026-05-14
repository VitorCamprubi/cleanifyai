package com.cleanifyai.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleanifyai.api.dto.auth.LoginRequest;
import com.cleanifyai.api.dto.auth.LoginResponse;
import com.cleanifyai.api.dto.auth.LogoutRequest;
import com.cleanifyai.api.dto.auth.RefreshTokenRequest;
import com.cleanifyai.api.dto.auth.RegisterCompanyRequest;
import com.cleanifyai.api.service.AuthService;
import com.cleanifyai.api.shared.web.RequestMetadata;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(
                request,
                RequestMetadata.clientIp(httpRequest),
                RequestMetadata.userAgent(httpRequest)));
    }

    @PostMapping("/register-company")
    public ResponseEntity<LoginResponse> registerCompany(
            @Valid @RequestBody RegisterCompanyRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.registerCompany(
                request,
                RequestMetadata.clientIp(httpRequest),
                RequestMetadata.userAgent(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.refresh(
                request,
                RequestMetadata.clientIp(httpRequest),
                RequestMetadata.userAgent(httpRequest)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
