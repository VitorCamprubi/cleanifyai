package com.cleanifyai.api.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanifyai.api.domain.entity.Agendamento;
import com.cleanifyai.api.domain.entity.Cliente;
import com.cleanifyai.api.domain.entity.Servico;
import com.cleanifyai.api.domain.enums.StatusAgendamento;
import com.cleanifyai.api.dto.agendamento.AgendamentoRequest;
import com.cleanifyai.api.dto.agendamento.AgendamentoResponse;
import com.cleanifyai.api.dto.agendamento.AtualizarStatusAgendamentoRequest;
import com.cleanifyai.api.dto.agendamento.ClienteResumoResponse;
import com.cleanifyai.api.dto.agendamento.ServicoResumoResponse;
import com.cleanifyai.api.exception.BusinessException;
import com.cleanifyai.api.exception.ResourceNotFoundException;
import com.cleanifyai.api.integration.whatsapp.NotificadorWhatsApp;
import com.cleanifyai.api.repository.AgendamentoRepository;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteService clienteService;
    private final ServicoService servicoService;
    private final NotificadorWhatsApp notificadorWhatsApp;

    public AgendamentoService(
            AgendamentoRepository agendamentoRepository,
            ClienteService clienteService,
            ServicoService servicoService,
            NotificadorWhatsApp notificadorWhatsApp) {
        this.agendamentoRepository = agendamentoRepository;
        this.clienteService = clienteService;
        this.servicoService = servicoService;
        this.notificadorWhatsApp = notificadorWhatsApp;
    }

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request) {
        Cliente cliente = clienteService.buscarEntidade(request.clienteId());
        Servico servico = servicoService.buscarEntidade(request.servicoId());
        validarServicoAtivo(servico);
        validarDataHorario(request.data(), request.horario());

        Agendamento agendamento = new Agendamento();
        preencherCampos(agendamento, request, cliente, servico);
        Agendamento salvo = agendamentoRepository.save(agendamento);
        // Futuro ponto de extensao para disparo de confirmacao automatica via WhatsApp.
        notificadorWhatsApp.notificarAgendamentoCriado(salvo);
        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar() {
        return agendamentoRepository.findAll(Sort.by(Sort.Direction.ASC, "data", "horario"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscarPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public AgendamentoResponse atualizar(Long id, AgendamentoRequest request) {
        Agendamento agendamento = buscarEntidade(id);
        Cliente cliente = clienteService.buscarEntidade(request.clienteId());
        Servico servico = servicoService.buscarEntidade(request.servicoId());
        validarAgendamentoEditavel(agendamento);
        validarServicoAtivoParaAtualizacao(agendamento, servico);
        validarDataHorario(request.data(), request.horario());
        validarTransicaoStatus(agendamento.getStatus(), obterStatusDestino(agendamento, request));

        preencherCampos(agendamento, request, cliente, servico);
        Agendamento salvo = agendamentoRepository.save(agendamento);
        notificadorWhatsApp.notificarAgendamentoAtualizado(salvo);
        return toResponse(salvo);
    }

    @Transactional
    public AgendamentoResponse atualizarStatus(Long id, AtualizarStatusAgendamentoRequest request) {
        Agendamento agendamento = buscarEntidade(id);
        validarTransicaoStatus(agendamento.getStatus(), request.status());
        agendamento.setStatus(request.status());
        Agendamento salvo = agendamentoRepository.save(agendamento);
        notificadorWhatsApp.notificarStatusAlterado(salvo);
        return toResponse(salvo);
    }

    @Transactional
    public AgendamentoResponse cancelar(Long id) {
        Agendamento agendamento = buscarEntidade(id);
        validarTransicaoStatus(agendamento.getStatus(), StatusAgendamento.CANCELADO);
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        Agendamento salvo = agendamentoRepository.save(agendamento);
        notificadorWhatsApp.notificarAgendamentoCancelado(salvo);
        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public Agendamento buscarEntidade(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento nao encontrado: " + id));
    }

    public EnumSet<StatusAgendamento> statusAtivosParaDashboard() {
        return EnumSet.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO, StatusAgendamento.EM_ANDAMENTO);
    }

    private void preencherCampos(Agendamento agendamento, AgendamentoRequest request, Cliente cliente, Servico servico) {
        agendamento.setCliente(cliente);
        agendamento.setServico(servico);
        agendamento.setData(request.data());
        agendamento.setHorario(request.horario());
        agendamento.setStatus(request.status() != null ? request.status() : StatusAgendamento.AGENDADO);
        agendamento.setObservacoes(normalizarTextoOpcional(request.observacoes()));
    }

    private void validarServicoAtivo(Servico servico) {
        if (!Boolean.TRUE.equals(servico.getAtivo())) {
            throw new BusinessException("Somente servicos ativos podem ser agendados");
        }
    }

    private void validarServicoAtivoParaAtualizacao(Agendamento agendamento, Servico servico) {
        if (Boolean.TRUE.equals(servico.getAtivo()) || agendamento.getServico().getId().equals(servico.getId())) {
            return;
        }

        throw new BusinessException("Somente servicos ativos podem ser vinculados ao agendamento");
    }

    private void validarAgendamentoEditavel(Agendamento agendamento) {
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO || agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new BusinessException("Nao e possivel editar um agendamento com status final");
        }
    }

    private void validarDataHorario(LocalDate data, LocalTime horario) {
        LocalDate hoje = LocalDate.now();
        if (data.isBefore(hoje)) {
            throw new BusinessException("Nao e possivel agendar para datas passadas");
        }

        if (data.isEqual(hoje) && horario.isBefore(LocalTime.now().withSecond(0).withNano(0))) {
            throw new BusinessException("Nao e possivel agendar horario passado para hoje");
        }
    }

    private StatusAgendamento obterStatusDestino(Agendamento agendamento, AgendamentoRequest request) {
        return request.status() != null ? request.status() : agendamento.getStatus();
    }

    private void validarTransicaoStatus(StatusAgendamento statusAtual, StatusAgendamento statusDestino) {
        if (statusAtual == statusDestino) {
            return;
        }

        boolean transicaoValida = switch (statusAtual) {
            case AGENDADO -> statusDestino == StatusAgendamento.CONFIRMADO || statusDestino == StatusAgendamento.CANCELADO;
            case CONFIRMADO -> statusDestino == StatusAgendamento.EM_ANDAMENTO || statusDestino == StatusAgendamento.CANCELADO;
            case EM_ANDAMENTO -> statusDestino == StatusAgendamento.CONCLUIDO || statusDestino == StatusAgendamento.CANCELADO;
            case CONCLUIDO, CANCELADO -> false;
        };

        if (!transicaoValida) {
            throw new BusinessException("Transicao de status invalida para o agendamento");
        }
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) {
            return null;
        }

        String normalizado = valor.trim().replaceAll("\\s{2,}", " ");
        return normalizado.isBlank() ? null : normalizado;
    }

    private AgendamentoResponse toResponse(Agendamento agendamento) {
        return new AgendamentoResponse(
                agendamento.getId(),
                new ClienteResumoResponse(
                        agendamento.getCliente().getId(),
                        agendamento.getCliente().getNome(),
                        agendamento.getCliente().getTelefone(),
                        agendamento.getCliente().getVeiculo(),
                        agendamento.getCliente().getPlaca()),
                new ServicoResumoResponse(
                        agendamento.getServico().getId(),
                        agendamento.getServico().getNome(),
                        agendamento.getServico().getPreco(),
                        agendamento.getServico().getDuracaoMinutos()),
                agendamento.getData(),
                agendamento.getHorario(),
                agendamento.getStatus(),
                agendamento.getObservacoes());
    }
}

