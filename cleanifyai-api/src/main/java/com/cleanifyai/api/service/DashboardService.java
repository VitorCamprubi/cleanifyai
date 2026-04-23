package com.cleanifyai.api.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanifyai.api.domain.entity.Agendamento;
import com.cleanifyai.api.dto.dashboard.DashboardResponse;
import com.cleanifyai.api.dto.dashboard.ProximoAgendamentoResponse;
import com.cleanifyai.api.repository.AgendamentoRepository;
import com.cleanifyai.api.repository.ClienteRepository;
import com.cleanifyai.api.repository.ServicoRepository;

@Service
public class DashboardService {

    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AgendamentoService agendamentoService;

    public DashboardService(
            ClienteRepository clienteRepository,
            ServicoRepository servicoRepository,
            AgendamentoRepository agendamentoRepository,
            AgendamentoService agendamentoService) {
        this.clienteRepository = clienteRepository;
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.agendamentoService = agendamentoService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse obterResumo() {
        LocalDate hoje = LocalDate.now();
        LocalTime agora = LocalTime.now().withSecond(0).withNano(0);

        List<ProximoAgendamentoResponse> proximosAgendamentos = agendamentoRepository
                .findTop10ByStatusInAndDataGreaterThanEqualOrderByDataAscHorarioAsc(
                        agendamentoService.statusAtivosParaDashboard(),
                        hoje)
                .stream()
                .filter(agendamento -> agendamento.getData().isAfter(hoje)
                        || !agendamento.getHorario().isBefore(agora))
                .limit(5)
                .map(this::toResponse)
                .toList();

        return new DashboardResponse(
                clienteRepository.count(),
                servicoRepository.countByAtivoTrue(),
                agendamentoRepository.countByData(hoje),
                proximosAgendamentos);
    }

    private ProximoAgendamentoResponse toResponse(Agendamento agendamento) {
        return new ProximoAgendamentoResponse(
                agendamento.getId(),
                agendamento.getCliente().getNome(),
                agendamento.getServico().getNome(),
                agendamento.getData(),
                agendamento.getHorario(),
                agendamento.getStatus());
    }
}

