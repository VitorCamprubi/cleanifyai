package com.cleanifyai.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

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
import com.cleanifyai.api.domain.entity.Servico;
import com.cleanifyai.api.domain.entity.User;
import com.cleanifyai.api.domain.enums.UserRole;
import com.cleanifyai.api.repository.AgendamentoRepository;
import com.cleanifyai.api.repository.ClienteRepository;
import com.cleanifyai.api.repository.ServicoRepository;
import com.cleanifyai.api.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityAndCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cliente clienteBase;
    private Servico servicoBase;

    @BeforeEach
    void setUp() {
        agendamentoRepository.deleteAll();
        clienteRepository.deleteAll();
        servicoRepository.deleteAll();
        userRepository.deleteAll();

        criarUsuario("Administrador CleanifyAI", "admin@cleanifyai.local", "admin123", UserRole.ADMIN);
        criarUsuario("Atendente CleanifyAI", "atendente@cleanifyai.local", "atendente123", UserRole.ATENDENTE);

        clienteBase = new Cliente();
        clienteBase.setEmpresaId(1L);
        clienteBase.setNome("Cliente Base");
        clienteBase.setTelefone("5511999991111");
        clienteBase.setEmail("cliente.base@teste.local");
        clienteBase.setVeiculo("Onix");
        clienteBase.setPlaca("ABC1D23");
        clienteBase = clienteRepository.save(clienteBase);

        servicoBase = new Servico();
        servicoBase.setEmpresaId(1L);
        servicoBase.setNome("Servico Base");
        servicoBase.setDescricao("Servico base para testes");
        servicoBase.setPreco(new BigDecimal("89.90"));
        servicoBase.setDuracaoMinutos(90);
        servicoBase.setAtivo(true);
        servicoBase = servicoRepository.save(servicoBase);
    }

    @Test
    void pingShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pong"));
    }

    @Test
    void loginShouldReturnJwtForAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@cleanifyai.local",
                                  "senha": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("admin@cleanifyai.local"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void loginShouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@cleanifyai.local",
                                  "senha": "senha-errada"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou senha invalidos"));
    }

    @Test
    void protectedEndpointsShouldRejectAnonymousAndInvalidToken() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Nao autenticado"));

        mockMvc.perform(get("/api/clientes")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Nao autenticado"));
    }

    @Test
    void atendenteShouldAccessAllowedResourcesAndBeBlockedFromServicoWrite() throws Exception {
        String atendenteToken = loginAndGetToken("atendente@cleanifyai.local", "atendente123");

        mockMvc.perform(get("/api/clientes")
                        .header("Authorization", bearer(atendenteToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Cliente Base"));

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(atendenteToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientes").value(1));

        mockMvc.perform(get("/api/servicos")
                        .header("Authorization", bearer(atendenteToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Servico Base"));

        mockMvc.perform(post("/api/servicos")
                        .header("Authorization", bearer(atendenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Polimento Tecnico",
                                  "descricao": "Teste",
                                  "preco": 120.00,
                                  "duracaoMinutos": 120,
                                  "ativo": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Sem permissao para acessar este recurso"));
    }

    @Test
    void adminShouldExecuteServicoCrud() throws Exception {
        String adminToken = loginAndGetToken("admin@cleanifyai.local", "admin123");

        MvcResult createResult = mockMvc.perform(post("/api/servicos")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Higienizacao Premium",
                                  "descricao": "Pacote completo",
                                  "preco": 150.00,
                                  "duracaoMinutos": 180,
                                  "ativo": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Higienizacao Premium"))
                .andReturn();

        Long servicoId = readId(createResult);

        mockMvc.perform(put("/api/servicos/{id}", servicoId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Higienizacao Premium Plus",
                                  "descricao": "Pacote atualizado",
                                  "preco": 199.90,
                                  "duracaoMinutos": 210,
                                  "ativo": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Higienizacao Premium Plus"));

        mockMvc.perform(delete("/api/servicos/{id}", servicoId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminShouldExecuteClienteCrud() throws Exception {
        String adminToken = loginAndGetToken("admin@cleanifyai.local", "admin123");

        MvcResult createResult = mockMvc.perform(post("/api/clientes")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Novo Cliente",
                                  "telefone": "(11) 98888-7777",
                                  "email": "novo.cliente@teste.local",
                                  "veiculo": "HB20",
                                  "placa": "BRA2E19",
                                  "observacoes": "Cliente de teste"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Novo Cliente"))
                .andReturn();

        Long clienteId = readId(createResult);

        mockMvc.perform(put("/api/clientes/{id}", clienteId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Novo Cliente Atualizado",
                                  "telefone": "(11) 97777-6666",
                                  "email": "novo.atualizado@teste.local",
                                  "veiculo": "HB20S",
                                  "placa": "BRA2E19",
                                  "observacoes": "Atualizado"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo Cliente Atualizado"))
                .andExpect(jsonPath("$.telefone").value("5511977776666"));

        mockMvc.perform(delete("/api/clientes/{id}", clienteId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void atendenteShouldExecuteAgendamentoLifecycle() throws Exception {
        String atendenteToken = loginAndGetToken("atendente@cleanifyai.local", "atendente123");

        LocalDate data = LocalDate.now().plusDays(1);

        MvcResult createResult = mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", bearer(atendenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": %d,
                                  "servicoId": %d,
                                  "data": "%s",
                                  "horario": "10:30:00",
                                  "status": "AGENDADO",
                                  "observacoes": "Agendamento inicial"
                                }
                                """.formatted(clienteBase.getId(), servicoBase.getId(), data)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andReturn();

        Long agendamentoId = readId(createResult);

        mockMvc.perform(put("/api/agendamentos/{id}", agendamentoId)
                        .header("Authorization", bearer(atendenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": %d,
                                  "servicoId": %d,
                                  "data": "%s",
                                  "horario": "11:15:00",
                                  "status": "AGENDADO",
                                  "observacoes": "Horario ajustado"
                                }
                                """.formatted(clienteBase.getId(), servicoBase.getId(), data.plusDays(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horario").value("11:15:00"));

        mockMvc.perform(patch("/api/agendamentos/{id}/status", agendamentoId)
                        .header("Authorization", bearer(atendenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONFIRMADO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));

        mockMvc.perform(patch("/api/agendamentos/{id}/cancelar", agendamentoId)
                        .header("Authorization", bearer(atendenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    void agendamentoShouldRejectPastDate() throws Exception {
        String adminToken = loginAndGetToken("admin@cleanifyai.local", "admin123");

        mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": %d,
                                  "servicoId": %d,
                                  "data": "%s",
                                  "horario": "%s",
                                  "status": "AGENDADO",
                                  "observacoes": "Teste invalido"
                                }
                                """.formatted(
                                clienteBase.getId(),
                                servicoBase.getId(),
                                LocalDate.now().minusDays(1),
                                LocalTime.of(10, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Data deve ser hoje ou futura")));
    }

    private void criarUsuario(String nome, String email, String senha, UserRole role) {
        User user = new User();
        user.setNome(nome);
        user.setEmail(email);
        user.setSenha(passwordEncoder.encode(senha));
        user.setRole(role);
        user.setAtivo(true);
        userRepository.save(user);
    }

    private String loginAndGetToken(String email, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "senha": "%s"
                                }
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
