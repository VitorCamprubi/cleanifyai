package com.cleanifyai.api.dto.dashboard;

import java.time.LocalDate;
import java.time.LocalTime;

import com.cleanifyai.api.domain.enums.StatusAgendamento;

public record ProximoAgendamentoResponse(
        Long id,
        String clienteNome,
        String servicoNome,
        LocalDate data,
        LocalTime horario,
        StatusAgendamento status) {
}

