package com.cleanifyai.api.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.cleanifyai.api.dto.auth.AuthUserResponse;
import com.cleanifyai.api.dto.auth.LoginRequest;
import com.cleanifyai.api.dto.auth.LoginResponse;
import com.cleanifyai.api.security.AuthenticatedUser;
import com.cleanifyai.api.security.JwtService;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(),
                        request.senha()))
                .getPrincipal();

        String token = jwtService.generateToken(authenticatedUser.getUser());

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.extractExpiration(token),
                new AuthUserResponse(
                        authenticatedUser.getUser().getId(),
                        authenticatedUser.getUser().getNome(),
                        authenticatedUser.getUser().getEmail(),
                        authenticatedUser.getUser().getRole()));
    }
}
