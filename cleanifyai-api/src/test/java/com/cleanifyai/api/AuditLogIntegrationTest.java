package com.cleanifyai.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.cleanifyai.api.domain.entity.AuditLog;
import com.cleanifyai.api.domain.entity.Empresa;
import com.cleanifyai.api.domain.entity.User;
import com.cleanifyai.api.domain.enums.UserRole;
import com.cleanifyai.api.repository.AuditLogRepository;
import com.cleanifyai.api.repository.ClienteRepository;
import com.cleanifyai.api.repository.EmpresaRepository;
import com.cleanifyai.api.repository.RefreshTokenRepository;
import com.cleanifyai.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AuditLogIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Empresa empresa;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        auditLogRepository.deleteAll();
        clienteRepository.deleteAll();
        userRepository.deleteAll();
        empresaRepository.deleteAll();

        empresa = new Empresa();
        empresa.setNome("Empresa Auditavel");
        empresa.setAtiva(true);
        empresa = empresaRepository.save(empresa);

        User user = new User();
        user.setEmpresaId(empresa.getId());
        user.setNome("Admin Auditoria");
        user.setEmail("admin.audit@teste.local");
        user.setSenha(passwordEncoder.encode("admin123"));
        user.setRole(UserRole.ADMIN);
        user.setAtivo(true);
        userRepository.save(user);
    }

    @Test
    void deveAuditarMutacaoAutenticadaSemPersistirPayload() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", "Bearer " + token)
                        .header("User-Agent", "CleanifyAI-Test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Cliente Auditavel",
                                  "telefone": "(11) 98888-7777",
                                  "email": "auditavel@teste.local",
                                  "observacoes": "Nao gravar payload sensivel"
                                }
                                """))
                .andExpect(status().isCreated());

        List<AuditLog> logs = auditLogRepository.findAllByEmpresaId(
                empresa.getId(),
                Sort.by(Sort.Direction.ASC, "occurredAt"));

        assertThat(logs).hasSize(1);
        AuditLog log = logs.get(0);
        assertThat(log.getEmpresaId()).isEqualTo(empresa.getId());
        assertThat(log.getUserEmail()).isEqualTo("admin.audit@teste.local");
        assertThat(log.getAction()).isEqualTo("CREATE");
        assertThat(log.getResourceType()).isEqualTo("clientes");
        assertThat(log.getHttpMethod()).isEqualTo("POST");
        assertThat(log.getPath()).isEqualTo("/api/clientes");
        assertThat(log.getStatusCode()).isEqualTo(201);
        assertThat(log.getUserAgent()).isEqualTo("CleanifyAI-Test");
        assertThat(log.getDurationMs()).isGreaterThanOrEqualTo(0);
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin.audit@teste.local", "senha": "admin123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
