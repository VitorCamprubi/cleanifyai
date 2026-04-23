package com.cleanifyai.api.dto.agendamento;

import com.cleanifyai.api.domain.enums.StatusAgendamento;

import jakarta.validation.constraints.NotNull;

public record AtualizarStatusAgendamentoRequest(
        @NotNull(message = "Status e obrigatorio")
        StatusAgendamento status) {
}

