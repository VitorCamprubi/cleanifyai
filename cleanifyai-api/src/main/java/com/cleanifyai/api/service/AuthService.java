package com.cleanifyai.api.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanifyai.api.domain.entity.Empresa;
import com.cleanifyai.api.domain.entity.User;
import com.cleanifyai.api.domain.enums.UserRole;
import com.cleanifyai.api.dto.auth.AuthUserResponse;
import com.cleanifyai.api.dto.auth.LoginRequest;
import com.cleanifyai.api.dto.auth.LoginResponse;
import com.cleanifyai.api.dto.auth.LogoutRequest;
import com.cleanifyai.api.dto.auth.RefreshTokenRequest;
import com.cleanifyai.api.dto.auth.RegisterCompanyRequest;
import com.cleanifyai.api.exception.BusinessException;
import com.cleanifyai.api.repository.EmpresaRepository;
import com.cleanifyai.api.repository.UserRepository;
import com.cleanifyai.api.security.AuthenticatedUser;
import com.cleanifyai.api.security.JwtService;
import com.cleanifyai.api.service.RefreshTokenService.RotatedRefreshToken;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmpresaRepository empresaRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            EmpresaRepository empresaRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            LoginRateLimiter loginRateLimiter) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.empresaRepository = empresaRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        loginRateLimiter.checkAllowed(email, ipAddress);

        AuthenticatedUser authenticatedUser;
        try {
            authenticatedUser = (AuthenticatedUser) authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.senha()))
                    .getPrincipal();
        } catch (RuntimeException ex) {
            loginRateLimiter.recordFailure(email, ipAddress);
            throw ex;
        }

        loginRateLimiter.recordSuccess(email, ipAddress);
        return buildLoginResponse(authenticatedUser.getUser(), ipAddress, userAgent);
    }

    @Transactional
    public LoginResponse registerCompany(RegisterCompanyRequest request, String ipAddress, String userAgent) {
        String emailAdmin = request.admin().email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(emailAdmin)) {
            throw new BusinessException("Ja existe um usuario com este email");
        }

        String cnpj = normalizarCnpj(request.empresa().cnpj());
        if (cnpj != null && empresaRepository.existsByCnpj(cnpj)) {
            throw new BusinessException("Ja existe uma empresa cadastrada com este CNPJ");
        }

        Empresa empresa = new Empresa();
        empresa.setNome(request.empresa().nome().trim());
        empresa.setCnpj(cnpj);
        empresa.setTelefone(normalizarOpcional(request.empresa().telefone()));
        empresa.setEmail(normalizarEmailOpcional(request.empresa().email()));
        empresa.setAtiva(true);
        empresa = empresaRepository.save(empresa);

        User admin = new User();
        admin.setEmpresaId(empresa.getId());
        admin.setNome(request.admin().nome().trim());
        admin.setEmail(emailAdmin);
        admin.setSenha(passwordEncoder.encode(request.admin().senha()));
        admin.setRole(UserRole.ADMIN);
        admin.setAtivo(true);
        admin = userRepository.save(admin);

        return buildLoginResponse(admin, ipAddress, userAgent);
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
        RotatedRefreshToken rotated = refreshTokenService.rotate(request.refreshToken(), ipAddress, userAgent);
        return buildLoginResponse(rotated.user(), rotated.token(), rotated.expiresAt());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private LoginResponse buildLoginResponse(User user, String ipAddress, String userAgent) {
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issueFor(user, ipAddress, userAgent);
        return buildLoginResponse(user, refreshToken.token(), refreshToken.expiresAt());
    }

    private LoginResponse buildLoginResponse(User user, String refreshToken, Instant refreshExpiresAt) {
        String token = jwtService.generateToken(user);
        String empresaNome = empresaRepository.findById(user.getEmpresaId())
                .map(Empresa::getNome)
                .orElse("Empresa");
        return new LoginResponse(
                token,
                "Bearer",
                jwtService.extractExpiration(token),
                refreshToken,
                refreshExpiresAt,
                new AuthUserResponse(
                        user.getId(),
                        user.getEmpresaId(),
                        empresaNome,
                        user.getNome(),
                        user.getEmail(),
                        user.getRole()));
    }

    private String normalizarCnpj(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        String digitos = cnpj.replaceAll("\\D", "");
        if (digitos.isBlank()) {
            return null;
        }
        if (digitos.length() != 14) {
            throw new BusinessException("CNPJ deve conter 14 digitos");
        }
        return digitos;
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.isBlank() ? null : normalizado;
    }

    private String normalizarEmailOpcional(String email) {
        String valor = normalizarOpcional(email);
        return valor != null ? valor.toLowerCase(Locale.ROOT) : null;
    }
}
