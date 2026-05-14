package com.cleanifyai.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
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

import com.cleanifyai.api.domain.entity.Agendamento;
import com.cleanifyai.api.domain.entity.CategoriaFinanceira;
import com.cleanifyai.api.domain.entity.Cliente;
import com.cleanifyai.api.domain.entity.Empresa;
import com.cleanifyai.api.domain.entity.Lancamento;
import com.cleanifyai.api.domain.entity.OrdemServico;
import com.cleanifyai.api.domain.entity.Servico;
import com.cleanifyai.api.domain.entity.User;
import com.cleanifyai.api.domain.entity.Veiculo;
import com.cleanifyai.api.domain.enums.FormaPagamento;
import com.cleanifyai.api.domain.enums.StatusAgendamento;
import com.cleanifyai.api.domain.enums.StatusOrdem;
import com.cleanifyai.api.domain.enums.TipoCategoria;
import com.cleanifyai.api.domain.enums.TipoLancamento;
import com.cleanifyai.api.domain.enums.UserRole;
import com.cleanifyai.api.repository.AgendamentoRepository;
import com.cleanifyai.api.repository.AuditLogRepository;
import com.cleanifyai.api.repository.CategoriaFinanceiraRepository;
import com.cleanifyai.api.repository.ClienteRepository;
import com.cleanifyai.api.repository.EmpresaRepository;
import com.cleanifyai.api.repository.LancamentoRepository;
import com.cleanifyai.api.repository.OrdemServicoRepository;
import com.cleanifyai.api.repository.RefreshTokenRepository;
import com.cleanifyai.api.repository.ServicoRepository;
import com.cleanifyai.api.repository.UserRepository;
import com.cleanifyai.api.repository.VeiculoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class TenantIsolationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private VeiculoRepository veiculoRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private OrdemServicoRepository ordemServicoRepository;
    @Autowired private LancamentoRepository lancamentoRepository;
    @Autowired private CategoriaFinanceiraRepository categoriaRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Fixture tenantA;
    private Fixture tenantB;

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
        categoriaRepository.deleteAll();
        userRepository.deleteAll();
        empresaRepository.deleteAll();

        tenantA = criarTenant("Tenant A", "admin.a@teste.local", "Cliente A", "Toyota", "Corolla", "AAA1A11", "Servico A", "101.00");
        tenantB = criarTenant("Tenant B", "admin.b@teste.local", "Cliente B", "Honda", "Civic", "BBB2B22", "Servico B", "202.00");
    }

    @Test
    void endpointsDeListagemELeituraDevemIsolarDadosPorEmpresa() throws Exception {
        String tokenA = login(tenantA.email);

        mockMvc.perform(get("/api/clientes").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(tenantA.cliente.getId()));

        mockMvc.perform(get("/api/clientes/{id}", tenantB.cliente.getId()).header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/veiculos").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(tenantA.veiculo.getId()));

        mockMvc.perform(get("/api/servicos").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(tenantA.servico.getId()));

        mockMvc.perform(get("/api/servicos/{id}", tenantB.servico.getId()).header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/agendamentos").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(tenantA.agendamento.getId()));

        mockMvc.perform(get("/api/agendamentos/{id}", tenantB.agendamento.getId()).header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/ordens").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(tenantA.ordem.getId()));

        mockMvc.perform(get("/api/ordens/{id}", tenantB.ordem.getId()).header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/financeiro/lancamentos").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(tenantA.lancamento.getId()));

        mockMvc.perform(get("/api/financeiro/resumo")
                        .header("Authorization", bearer(tokenA))
                        .param("inicio", LocalDate.now().toString())
                        .param("fim", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntradas").value(101.00))
                .andExpect(jsonPath("$.quantidadeLancamentos").value(1));

        mockMvc.perform(get("/api/financeiro/categorias").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(tenantA.categoria.getId()));

        mockMvc.perform(get("/api/financeiro/categorias/{id}", tenantB.categoria.getId()).header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void escritasNaoDevemAceitarIdsDeOutroTenant() throws Exception {
        String tokenA = login(tenantA.email);

        mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": %d,
                                  "servicoId": %d,
                                  "veiculoId": %d,
                                  "data": "%s",
                                  "horario": "15:00:00",
                                  "status": "AGENDADO"
                                }
                                """.formatted(
                                tenantA.cliente.getId(),
                                tenantA.servico.getId(),
                                tenantB.veiculo.getId(),
                                LocalDate.now().plusDays(2))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/ordens")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": %d,
                                  "veiculoId": %d,
                                  "itens": [{"servicoId": %d, "quantidade": 1, "valorUnitario": 101.00}]
                                }
                                """.formatted(tenantA.cliente.getId(), tenantB.veiculo.getId(), tenantA.servico.getId())))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/financeiro/lancamentos")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "ENTRADA",
                                  "valor": 101.00,
                                  "formaPagamento": "PIX",
                                  "dataLancamento": "%s",
                                  "descricao": "Tentativa cross-tenant",
                                  "ordemId": %d
                                }
                                """.formatted(LocalDate.now(), tenantB.ordem.getId())))
                .andExpect(status().isNotFound());
    }

    private Fixture criarTenant(
            String empresaNome,
            String email,
            String clienteNome,
            String marca,
            String modelo,
            String placa,
            String servicoNome,
            String valor) {
        Empresa empresa = new Empresa();
        empresa.setNome(empresaNome);
        empresa.setAtiva(true);
        empresa = empresaRepository.save(empresa);

        User user = new User();
        user.setEmpresaId(empresa.getId());
        user.setNome("Admin " + empresaNome);
        user.setEmail(email);
        user.setSenha(passwordEncoder.encode("admin123"));
        user.setRole(UserRole.ADMIN);
        user.setAtivo(true);
        userRepository.save(user);

        Cliente cliente = new Cliente();
        cliente.setEmpresaId(empresa.getId());
        cliente.setNome(clienteNome);
        cliente.setTelefone("5511999990000");
        cliente.setAtivo(true);
        cliente = clienteRepository.save(cliente);

        Veiculo veiculo = new Veiculo();
        veiculo.setEmpresaId(empresa.getId());
        veiculo.setClienteId(cliente.getId());
        veiculo.setMarca(marca);
        veiculo.setModelo(modelo);
        veiculo.setPlaca(placa);
        veiculo.setAtivo(true);
        veiculo = veiculoRepository.save(veiculo);

        Servico servico = new Servico();
        servico.setEmpresaId(empresa.getId());
        servico.setNome(servicoNome);
        servico.setPreco(new BigDecimal(valor));
        servico.setDuracaoMinutos(60);
        servico.setAtivo(true);
        servico = servicoRepository.save(servico);

        Agendamento agendamento = new Agendamento();
        agendamento.setEmpresaId(empresa.getId());
        agendamento.setCliente(cliente);
        agendamento.setVeiculo(veiculo);
        agendamento.setServico(servico);
        agendamento.setData(LocalDate.now().plusDays(1));
        agendamento.setHorario(LocalTime.of(10, 0));
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamento = agendamentoRepository.save(agendamento);

        OrdemServico ordem = new OrdemServico();
        ordem.setEmpresaId(empresa.getId());
        ordem.setCliente(cliente);
        ordem.setVeiculo(veiculo);
        ordem.setAgendamento(agendamento);
        ordem.setStatus(StatusOrdem.ABERTA);
        ordem.setValorTotal(new BigDecimal(valor));
        ordem.setAbertaEm(Instant.now());
        ordem = ordemServicoRepository.save(ordem);

        CategoriaFinanceira categoria = new CategoriaFinanceira();
        categoria.setEmpresaId(empresa.getId());
        categoria.setNome("Servicos " + empresaNome);
        categoria.setTipo(TipoCategoria.RECEITA);
        categoria.setCor("#18E4D3");
        categoria.setAtivo(true);
        categoria = categoriaRepository.save(categoria);

        Lancamento lancamento = new Lancamento();
        lancamento.setEmpresaId(empresa.getId());
        lancamento.setTipo(TipoLancamento.ENTRADA);
        lancamento.setValor(new BigDecimal(valor));
        lancamento.setFormaPagamento(FormaPagamento.PIX);
        lancamento.setDataLancamento(LocalDate.now());
        lancamento.setDescricao("Receita " + empresaNome);
        lancamento.setOrdemId(ordem.getId());
        lancamento.setCategoriaId(categoria.getId());
        lancamento = lancamentoRepository.save(lancamento);

        return new Fixture(email, cliente, veiculo, servico, agendamento, ordem, categoria, lancamento);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "admin123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Fixture(
            String email,
            Cliente cliente,
            Veiculo veiculo,
            Servico servico,
            Agendamento agendamento,
            OrdemServico ordem,
            CategoriaFinanceira categoria,
            Lancamento lancamento) {
    }
}
