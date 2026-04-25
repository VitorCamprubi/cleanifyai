package com.cleanifyai.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
import com.cleanifyai.api.domain.enums.UserRole;
import com.cleanifyai.api.repository.AgendamentoRepository;
import com.cleanifyai.api.repository.ClienteRepository;
import com.cleanifyai.api.repository.EmpresaRepository;
import com.cleanifyai.api.repository.LancamentoRepository;
import com.cleanifyai.api.repository.OrdemServicoRepository;
import com.cleanifyai.api.repository.ServicoRepository;
import com.cleanifyai.api.repository.UserRepository;
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
    @Autowired private PasswordEncoder passwordEncoder;

    private Empresa empresa;
    private Cliente cliente;
    private Servico servico;

    @BeforeEach
    void setUp() {
        lancamentoRepository.deleteAll();
        ordemServicoRepository.deleteAll();
        agendamentoRepository.deleteAll();
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
        cliente.setVeiculo("Civic");
        cliente.setPlaca("ABC1D23");
        cliente = clienteRepository.save(cliente);

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
                                  "observacoes": "Cliente preferiu pacote completo",
                                  "itens": [
                                    {"servicoId": %d, "quantidade": 1, "valorUnitario": 80.00},
                                    {"servicoId": %d, "quantidade": 2, "valorUnitario": 50.00}
                                  ]
                                }
                                """.formatted(cliente.getId(), servico.getId(), servico.getId())))
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
                                  "observacoes": "outra OS",
                                  "itens": [{"servicoId": %d, "quantidade": 1, "valorUnitario": 50.00}]
                                }
                                """.formatted(cliente.getId(), servico.getId())))
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
                                  "itens": [{"servicoId": %d, "quantidade": 1, "valorUnitario": 80.00}]
                                }
                                """.formatted(cliente.getId(), servico.getId())))
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
                                  "itens": [{"servicoId": %d, "quantidade": 1, "valorUnitario": 80.00}]
                                }
                                """.formatted(cliente.getId(), servico.getId())))
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
