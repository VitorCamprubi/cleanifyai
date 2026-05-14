package com.cleanifyai.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.cleanifyai.api.domain.entity.Cliente;
import com.cleanifyai.api.domain.entity.Empresa;
import com.cleanifyai.api.domain.entity.Servico;
import com.cleanifyai.api.domain.entity.User;
import com.cleanifyai.api.domain.entity.Veiculo;
import com.cleanifyai.api.domain.enums.UserRole;
import com.cleanifyai.api.repository.AgendamentoRepository;
import com.cleanifyai.api.repository.AuditLogRepository;
import com.cleanifyai.api.repository.ClienteRepository;
import com.cleanifyai.api.repository.EmpresaRepository;
import com.cleanifyai.api.repository.LancamentoRepository;
import com.cleanifyai.api.repository.OrdemServicoRepository;
import com.cleanifyai.api.repository.RefreshTokenRepository;
import com.cleanifyai.api.repository.ServicoRepository;
import com.cleanifyai.api.repository.UserRepository;
import com.cleanifyai.api.repository.VeiculoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class OrdemServicoIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private OrdemServicoRepository ordemServicoRepository;
    @Autowired private LancamentoRepository lancamentoRepository;
    @Autowired private VeiculoRepository veiculoRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Empresa empresa;
    private Cliente cliente;
    private Veiculo veiculo;
    private Servico servico;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        auditLogRepository.deleteAll();
        lancamentoRepository.deleteAll();
        ordemServicoRepository.deleteAll();
        agendamentoRepository.deleteAll();
        veiculoRepository.deleteAll();
        clienteRepository.deleteAll();
        servicoRepository.deleteAll();
        userRepository.deleteAll();
        empresaRepository.deleteAll();

        empresa = new Empresa();
        empresa.setNome("Estetica Teste");
        empresa.setAtiva(true);
        empresa = empresaRepository.save(empresa);

        criarUsuario("Admin OS", "admin.os@teste.local", "admin123", UserRole.ADMIN);
        criarUsuario("Atendente OS", "atendente.os@teste.local", "atendente123", UserRole.ATENDENTE);

        cliente = new Cliente();
        cliente.setEmpresaId(empresa.getId());
        cliente.setNome("Cliente OS");
        cliente.setTelefone("5511988887777");
        cliente = clienteRepository.save(cliente);

        veiculo = new Veiculo();
        veiculo.setEmpresaId(empresa.getId());
        veiculo.setClienteId(cliente.getId());
        veiculo.setMarca("Honda");
        veiculo.setModelo("Civic");
        veiculo.setPlaca("ABC1D23");
        veiculo.setCor("Prata");
        veiculo.setAnoModelo(2024);
        veiculo.setAtivo(true);
        veiculo = veiculoRepository.save(veiculo);

        servico = new Servico();
        servico.setEmpresaId(empresa.getId());
        servico.setNome("Lavagem Tecnica");
        servico.setPreco(new BigDecimal("80.00"));
        servico.setDuracaoMinutos(60);
        servico.setAtivo(true);
        servico = servicoRepository.save(servico);
    }

    @Test
    void deveCriarOSComItensECalcularValorTotal() throws Exception {
        String token = loginAdmin();

        MvcResult result = mockMvc.perform(post("/api/ordens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": %d,
                                  "veiculoId": %d,
                                  "observacoes": "Cliente preferiu pacote completo",
                                  "itens": [
                                    {"servicoId": %d, "quantidade": 1, "valorUnitario": 80.00},
                                    {"servicoId": %d, "quantidade": 2, "valorUnitario": 50.00}
                                  ]
                                }
                                """.formatted(cliente.getId(), veiculo.getId(), servico.getId(), servico.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.valorTotal").value(180.00))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andReturn();

        Long ordemId = readId(result);

        mockMvc.perform(get("/api/ordens/" + ordemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteNome").value("Cliente OS"))
                .andExpect(jsonPath("$.itens[0].descricao").value("Lavagem Tecnica"));
    }

    @Test
    void deveCriarOSAPartirDeAgendamento() throws Exception {
        String token = loginAdmin();
        LocalDate data = LocalDate.now().plusDays(1);

        MvcResult agendamentoResult = mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": %d,
                                  "servicoId": %d,
                                  "veiculoId": %d,
                                  "data": "%s",
                                  "horario": "09:00:00",
                                  "status": "CONFIRMADO",
                                  "observacoes": "Criar OS no check-in"
                                }
                                """.formatted(cliente.getId(), servico.getId(), veiculo.getId(), data)))
                .andExpect(status().isCreated())
                .andReturn();

        Long agendamentoId = readId(agendamentoResult);

        mockMvc.perform(post("/api/ordens/from-agendamento/{agendamentoId}", agendamentoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agendamentoId").value(agendamentoId))
                .andExpect(jsonPath("$.clienteId").value(cliente.getId()))
                .andExpect(jsonPath("$.veiculoId").value(veiculo.getId()))
                .andExpect(jsonPath("$.valorTotal").value(80.00))
                .andExpect(jsonPath("$.itens.length()").value(1));
    }

    @Test
    void deveExecutarMaquinaDeEstadosCompleta() throws Exception {
        String token = loginAdmin();
        Long ordemId = criarOrdem(token);

        // ABERTA -> EM_EXECUCAO
        mockMvc.perform(patch("/api/ordens/" + ordemId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"EM_EXECUCAO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));

        // EM_EXECUCAO -> CONCLUIDA (deve registrar fechadaEm)
        mockMvc.perform(patch("/api/ordens/" + ordemId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONCLUIDA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.fechadaEm").isNotEmpty());

        // CONCLUIDA -> ENTREGUE
        mockMvc.perform(patch("/api/ordens/" + ordemId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ENTREGUE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));

        mockMvc.perform(get("/api/financeiro/lancamentos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ordemId").value(ordemId))
                .andExpect(jsonPath("$[0].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$[0].valor").value(80.00));
    }

    @Test
    void deveRejeitarTransicaoInvalida() throws Exception {
        String token = loginAdmin();
        Long ordemId = criarOrdem(token);

        // ABERTA -> ENTREGUE (deve falhar)
        mockMvc.perform(patch("/api/ordens/" + ordemId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ENTREGUE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Transicao de status invalida")));
    }

    @Test
    void deveBloquearEdicaoAposStatusFinal() throws Exception {
        String token = loginAdmin();
        Long ordemId = criarOrdem(token);

        mockMvc.perform(patch("/api/ordens/" + ordemId + "/cancelar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        // PUT em ordem cancelada deve falhar
        mockMvc.perform(post("/api/ordens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": %d,
                                  "veiculoId": %d,
                                  "observacoes": "outra OS",
                                  "itens": [{"servicoId": %d, "quantidade": 1, "valorUnitario": 50.00}]
                                }
                                """.formatted(cliente.getId(), veiculo.getId(), servico.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void atendenteDeveConseguirOperarOS() throws Exception {
        String token = loginAndGetToken("atendente.os@teste.local", "atendente123");

        MvcResult result = mockMvc.perform(post("/api/ordens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": %d,
                                  "veiculoId": %d,
                                  "itens": [{"servicoId": %d, "quantidade": 1, "valorUnitario": 80.00}]
                                }
                                """.formatted(cliente.getId(), veiculo.getId(), servico.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        Long ordemId = readId(result);

        mockMvc.perform(patch("/api/ordens/" + ordemId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"EM_EXECUCAO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveExigirAutenticacao() throws Exception {
        mockMvc.perform(get("/api/ordens"))
                .andExpect(status().isUnauthorized());
    }

    private Long criarOrdem(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/ordens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": %d,
                                  "veiculoId": %d,
                                  "itens": [{"servicoId": %d, "quantidade": 1, "valorUnitario": 80.00}]
                                }
                                """.formatted(cliente.getId(), veiculo.getId(), servico.getId())))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
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
        return loginAndGetToken("admin.os@teste.local", "admin123");
    }

    private String loginAndGetToken(String email, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "%s"}
                                """.formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private Long readId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }
}
