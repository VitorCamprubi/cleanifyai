package com.cleanifyai.api.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cleanifyai.api.domain.entity.Agendamento;
import com.cleanifyai.api.domain.entity.Cliente;
import com.cleanifyai.api.domain.entity.Empresa;
import com.cleanifyai.api.domain.entity.Servico;
import com.cleanifyai.api.domain.entity.User;
import com.cleanifyai.api.domain.entity.Veiculo;
import com.cleanifyai.api.domain.enums.StatusAgendamento;
import com.cleanifyai.api.domain.enums.UserRole;
import com.cleanifyai.api.repository.AgendamentoRepository;
import com.cleanifyai.api.repository.ClienteRepository;
import com.cleanifyai.api.repository.EmpresaRepository;
import com.cleanifyai.api.repository.ServicoRepository;
import com.cleanifyai.api.repository.UserRepository;
import com.cleanifyai.api.repository.VeiculoRepository;

@Configuration
public class SeedDataConfig {

    @Bean
    CommandLineRunner loadSeedData(
            AppProperties appProperties,
            EmpresaRepository empresaRepository,
            UserRepository userRepository,
            ClienteRepository clienteRepository,
            ServicoRepository servicoRepository,
            AgendamentoRepository agendamentoRepository,
            VeiculoRepository veiculoRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (!appProperties.getSeed().isEnabled()) {
                return;
            }

            Empresa empresa = obterOuCriarEmpresaPadrao(empresaRepository);

            criarUsuarioPadraoSeNecessario(
                    userRepository,
                    passwordEncoder,
                    empresa.getId(),
                    "Administrador CleanifyAI",
                    "admin@cleanifyai.local",
                    "admin123",
                    UserRole.ADMIN);
            criarUsuarioPadraoSeNecessario(
                    userRepository,
                    passwordEncoder,
                    empresa.getId(),
                    "Atendente CleanifyAI",
                    "atendente@cleanifyai.local",
                    "atendente123",
                    UserRole.ATENDENTE);

            if (clienteRepository.count() > 0 || servicoRepository.count() > 0) {
                return;
            }

            Cliente cliente1 = new Cliente();
            cliente1.setEmpresaId(empresa.getId());
            cliente1.setNome("Carlos Almeida");
            cliente1.setTelefone("5511999990001");
            cliente1.setEmail("carlos@cliente.com");
            cliente1.setVeiculo("Honda Civic");
            cliente1.setPlaca("ABC1D23");
            cliente1.setObservacoes("Cliente recorrente de higienizacao interna");

            Cliente cliente2 = new Cliente();
            cliente2.setEmpresaId(empresa.getId());
            cliente2.setNome("Mariana Costa");
            cliente2.setTelefone("5511999990002");
            cliente2.setEmail("mariana@cliente.com");
            cliente2.setVeiculo("Jeep Compass");
            cliente2.setPlaca("EFG4H56");
            cliente2.setObservacoes("Prefere confirmacao no dia anterior");

            cliente1 = clienteRepository.save(cliente1);
            cliente2 = clienteRepository.save(cliente2);

            Veiculo veiculo1 = new Veiculo();
            veiculo1.setEmpresaId(empresa.getId());
            veiculo1.setClienteId(cliente1.getId());
            veiculo1.setMarca("Honda");
            veiculo1.setModelo("Civic");
            veiculo1.setPlaca("ABC1D23");
            veiculo1.setCor("Prata");
            veiculo1.setAnoModelo(2021);
            veiculo1.setAtivo(true);
            veiculoRepository.save(veiculo1);

            Veiculo veiculo2 = new Veiculo();
            veiculo2.setEmpresaId(empresa.getId());
            veiculo2.setClienteId(cliente2.getId());
            veiculo2.setMarca("Jeep");
            veiculo2.setModelo("Compass");
            veiculo2.setPlaca("EFG4H56");
            veiculo2.setCor("Branco");
            veiculo2.setAnoModelo(2023);
            veiculo2.setAtivo(true);
            veiculoRepository.save(veiculo2);

            Servico servico1 = new Servico();
            servico1.setEmpresaId(empresa.getId());
            servico1.setNome("Lavagem Premium");
            servico1.setDescricao("Lavagem externa completa com acabamento premium");
            servico1.setPreco(new BigDecimal("89.90"));
            servico1.setDuracaoMinutos(90);
            servico1.setAtivo(true);

            Servico servico2 = new Servico();
            servico2.setEmpresaId(empresa.getId());
            servico2.setNome("Vitrificacao de Pintura");
            servico2.setDescricao("Protecao de pintura com foco em brilho e durabilidade");
            servico2.setPreco(new BigDecimal("599.00"));
            servico2.setDuracaoMinutos(240);
            servico2.setAtivo(true);

            servico1 = servicoRepository.save(servico1);
            servico2 = servicoRepository.save(servico2);

            Agendamento agendamento1 = new Agendamento();
            agendamento1.setEmpresaId(empresa.getId());
            agendamento1.setCliente(cliente1);
            agendamento1.setServico(servico1);
            agendamento1.setData(LocalDate.now());
            agendamento1.setHorario(LocalTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0));
            agendamento1.setStatus(StatusAgendamento.CONFIRMADO);
            agendamento1.setObservacoes("Confirmado por telefone");

            Agendamento agendamento2 = new Agendamento();
            agendamento2.setEmpresaId(empresa.getId());
            agendamento2.setCliente(cliente2);
            agendamento2.setServico(servico2);
            agendamento2.setData(LocalDate.now().plusDays(1));
            agendamento2.setHorario(LocalTime.of(10, 30));
            agendamento2.setStatus(StatusAgendamento.AGENDADO);
            agendamento2.setObservacoes("Apresentar opcoes de manutencao recorrente");

            agendamentoRepository.save(agendamento1);
            agendamentoRepository.save(agendamento2);
        };
    }

    private Empresa obterOuCriarEmpresaPadrao(EmpresaRepository empresaRepository) {
        return empresaRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    Empresa empresa = new Empresa();
                    empresa.setNome("CleanifyAI Demo");
                    empresa.setCnpj(null);
                    empresa.setEmail("contato@cleanifyai.local");
                    empresa.setTelefone("5511999990000");
                    empresa.setAtiva(true);
                    return empresaRepository.save(empresa);
                });
    }

    private void criarUsuarioPadraoSeNecessario(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Long empresaId,
            String nome,
            String email,
            String senha,
            UserRole role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = new User();
        user.setEmpresaId(empresaId);
        user.setNome(nome);
        user.setEmail(email);
        user.setSenha(passwordEncoder.encode(senha));
        user.setRole(role);
        user.setAtivo(true);
        userRepository.save(user);
    }
}
