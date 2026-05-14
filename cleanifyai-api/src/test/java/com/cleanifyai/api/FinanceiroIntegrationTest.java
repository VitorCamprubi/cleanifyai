package com.cleanifyai.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import com.cleanifyai.api.repository.EmpresaRepository;
import com.cleanifyai.api.repository.AuditLogRepository;
import com.cleanifyai.api.repository.LancamentoRepository;
import com.cleanifyai.api.repository.RefreshTokenRepository;
import com.cleanifyai.api.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class FinanceiroIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private LancamentoRepository lancamentoRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Empresa empresa;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        auditLogRepository.deleteAll();
        lancamentoRepository.deleteAll();
        userRepository.deleteAll();
        empresaRepository.deleteAll();

        empresa = new Empresa();
        empresa.setNome("Estetica Financeiro");
        empresa.setAtiva(true);
        empresa = empresaRepository.save(empresa);

        criarUsuario("Admin Fin", "admin.fin@teste.local", "admin123", UserRole.ADMIN);
        criarUsuario("Atendente Fin", "atendente.fin@teste.local", "atendente123", UserRole.ATENDENTE);
    }

    @Test
    void adminDeveRegistrarLancamentoEntradaERefletirNoResumo() throws Exception {
        String token = loginAdmin();
        LocalDate hoje = LocalDate.now();

        mockMvc.perform(post("/api/financeiro/lancamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "ENTRADA",
                                  "valor": 150.50,
                                  "formaPagamento": "PIX",
                                  "dataLancamento": "%s",
                                  "descricao": "Lavagem premium"
                                }
                                """.formatted(hoje)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valor").value(150.50))
                .andExpect(jsonPath("$.formaPagamento").value("PIX"));

        mockMvc.perform(get("/api/financeiro/resumo")
                        .header("Authorization", "Bearer " + token)
                        .param("inicio", hoje.toString())
                        .param("fim", hoje.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntradas").value(150.50))
                .andExpect(jsonPath("$.totalSaidas").value(0))
                .andExpect(jsonPath("$.saldo").value(150.50))
                .andExpect(jsonPath("$.quantidadeLancamentos").value(1))
                .andExpect(jsonPath("$.porForma[0].formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.porForma[0].entradas").value(150.50));
    }

    @Test
    void atendenteDeveLerMasNaoDeveRegistrar() throws Exception {
        String tokenAtendente = loginAndGetToken("atendente.fin@teste.local", "atendente123");

        // GET ok
        mockMvc.perform(get("/api/financeiro/lancamentos")
                        .header("Authorization", "Bearer " + tokenAtendente))
                .andExpect(status().isOk());

        // POST forbidden
        mockMvc.perform(post("/api/financeiro/lancamentos")
                        .header("Authorization", "Bearer " + tokenAtendente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "ENTRADA",
                                  "valor": 100.00,
                                  "formaPagamento": "DINHEIRO",
                                  "dataLancamento": "%s",
                                  "descricao": "Tentativa atendente"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRejeitarValorZeroOuNegativo() throws Exception {
        String token = loginAdmin();

        mockMvc.perform(post("/api/financeiro/lancamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "ENTRADA",
                                  "valor": 0,
                                  "formaPagamento": "DINHEIRO",
                                  "dataLancamento": "%s",
                                  "descricao": "Invalido"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarDataFutura() throws Exception {
        String token = loginAdmin();

        mockMvc.perform(post("/api/financeiro/lancamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "ENTRADA",
                                  "valor": 100.00,
                                  "formaPagamento": "PIX",
                                  "dataLancamento": "%s",
                                  "descricao": "Futuro"
                                }
                                """.formatted(LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("data futura")));
    }

    @Test
    void deveEstornarLancamentoNoMesmoDia() throws Exception {
        String token = loginAdmin();
        LocalDate hoje = LocalDate.now();

        MvcResult result = mockMvc.perform(post("/api/financeiro/lancamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "SAIDA",
                                  "valor": 30.00,
                                  "formaPagamento": "DINHEIRO",
                                  "dataLancamento": "%s",
                                  "descricao": "Compra de produto"
                                }
                                """.formatted(hoje)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long id = json.get("id").asLong();

        mockMvc.perform(delete("/api/financeiro/lancamentos/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/financeiro/lancamentos")
                        .header("Authorization", "Bearer " + token)
                        .param("inicio", hoje.toString())
                        .param("fim", hoje.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void resumoDeveAgregarPorFormaQuandoMultiplasFormas() throws Exception {
        String token = loginAdmin();
        LocalDate hoje = LocalDate.now();

        registrarLancamento(token, "ENTRADA", "100.00", "PIX", "Lavagem 1");
        registrarLancamento(token, "ENTRADA", "200.00", "DINHEIRO", "Lavagem 2");
        registrarLancamento(token, "SAIDA", "50.00", "DINHEIRO", "Material");

        mockMvc.perform(get("/api/financeiro/resumo")
                        .header("Authorization", "Bearer " + token)
                        .param("inicio", hoje.toString())
                        .param("fim", hoje.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntradas").value(300.00))
                .andExpect(jsonPath("$.totalSaidas").value(50.00))
                .andExpect(jsonPath("$.saldo").value(250.00))
                .andExpect(jsonPath("$.quantidadeLancamentos").value(3))
                .andExpect(jsonPath("$.porForma.length()").value(2));
    }

    private void registrarLancamento(String token, String tipo, String valor, String forma, String descricao) throws Exception {
        mockMvc.perform(post("/api/financeiro/lancamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "%s",
                                  "valor": %s,
                                  "formaPagamento": "%s",
                                  "dataLancamento": "%s",
                                  "descricao": "%s"
                                }
                                """.formatted(tipo, valor, forma, LocalDate.now(), descricao)))
                .andExpect(status().isCreated());
    }

    private void criarUsuario(String nome, String email, String senha, UserRole role) {
        User user = new User();
        user.setEmpresaId(empresa.getId());
        user.setNome(nome);
        user.setEmail(email);
        user.setSenha(passwordEncoder.encode(senha));
        user.setRole(role);
        user.setAtivo(true);
        userRepository.save(user);
    }

    private String loginAdmin() throws Exception {
        return loginAndGetToken("admin.fin@teste.local", "admin123");
    }

    private String loginAndGetToken(String email, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "%s"}
                                """.formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @SuppressWarnings("unused")
    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
