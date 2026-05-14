package com.cleanifyai.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.cleanifyai.api.domain.entity.Empresa;
import com.cleanifyai.api.domain.entity.User;
import com.cleanifyai.api.domain.enums.UserRole;
import com.cleanifyai.api.repository.AuditLogRepository;
import com.cleanifyai.api.repository.EmpresaRepository;
import com.cleanifyai.api.repository.RefreshTokenRepository;
import com.cleanifyai.api.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "app.security.rate-limit.login-max-attempts=2",
        "app.security.rate-limit.login-window-seconds=900"
})
@AutoConfigureMockMvc
class AuthSessionAndHealthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        empresaRepository.deleteAll();

        Empresa empresa = new Empresa();
        empresa.setNome("Empresa Auth");
        empresa.setAtiva(true);
        empresa = empresaRepository.save(empresa);

        User user = new User();
        user.setEmpresaId(empresa.getId());
        user.setNome("Admin Auth");
        user.setEmail("admin.auth@teste.local");
        user.setSenha(passwordEncoder.encode("admin123"));
        user.setRole(UserRole.ADMIN);
        user.setAtivo(true);
        userRepository.save(user);
    }

    @Test
    void healthReadinessELivenessDevemSerPublicos() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void refreshTokenDeveSerRotacionadoERevogadoNoLogout() throws Exception {
        JsonNode login = login("10.10.0.1");
        String accessToken = login.get("token").asText();
        String refreshToken = login.get("refreshToken").asText();

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(login.get("refreshExpiresAt").asText()).isNotBlank();

        JsonNode refreshed = refresh(refreshToken, "10.10.0.1");
        String rotatedAccessToken = refreshed.get("token").asText();
        String rotatedRefreshToken = refreshed.get("refreshToken").asText();

        assertThat(rotatedAccessToken).isNotBlank();
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Sessao expirada. Faca login novamente."));

        mockMvc.perform(get("/api/clientes")
                        .header("Authorization", "Bearer " + rotatedAccessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginDeveAplicarRateLimitPorEmailEIpAposFalhas() throws Exception {
        String invalidLogin = """
                {
                  "email": "admin.auth@teste.local",
                  "senha": "senha-errada"
                }
                """;

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", "10.10.0.99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidLogin))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.10.0.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidLogin))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Muitas tentativas de login. Aguarde alguns minutos e tente novamente."));
    }

    private JsonNode login(String ipAddress) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", ipAddress)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin.auth@teste.local",
                                  "senha": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode refresh(String refreshToken, String ipAddress) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Forwarded-For", ipAddress)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
