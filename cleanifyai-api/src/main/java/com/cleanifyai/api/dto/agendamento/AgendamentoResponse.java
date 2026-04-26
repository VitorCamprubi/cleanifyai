package com.cleanifyai.api.dto.agendamento;

import java.time.LocalDate;
import java.time.LocalTime;

import com.cleanifyai.api.domain.enums.StatusAgendamento;

public record AgendamentoResponse(
        Long id,
        ClienteResumoResponse cliente,
        ServicoResumoResponse servico,
        VeiculoResumoResponse veiculo,
        LocalDate data,
        LocalTime horario,
        StatusAgendamento status,
        String observacoes) {
}

