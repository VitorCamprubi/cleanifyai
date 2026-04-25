package com.cleanifyai.api.service;

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
import com.cleanifyai.api.dto.auth.RegisterCompanyRequest;
import com.cleanifyai.api.exception.BusinessException;
import com.cleanifyai.api.repository.EmpresaRepository;
import com.cleanifyai.api.repository.UserRepository;
import com.cleanifyai.api.security.AuthenticatedUser;
import com.cleanifyai.api.security.JwtService;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmpresaRepository empresaRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            EmpresaRepository empresaRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.empresaRepository = empresaRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(),
                        request.senha()))
                .getPrincipal();

        return buildLoginResponse(authenticatedUser.getUser());
    }

    @Transactional
    public LoginResponse registerCompany(RegisterCompanyRequest request) {
        String emailAdmin = request.admin().email().trim().toLowerCase();

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

        return buildLoginResponse(admin);
    }

    private LoginResponse buildLoginResponse(User user) {
        String token = jwtService.generateToken(user);
        return new LoginResponse(
                token,
                "Bearer",
                jwtService.extractExpiration(token),
                new AuthUserResponse(
                        user.getId(),
                        user.getEmpresaId(),
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
        return valor != null ? valor.toLowerCase() : null;
    }
}
