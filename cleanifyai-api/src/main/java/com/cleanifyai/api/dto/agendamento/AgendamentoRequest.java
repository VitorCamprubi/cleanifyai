package com.cleanifyai.api.dto.agendamento;

import java.time.LocalDate;
import java.time.LocalTime;

import com.cleanifyai.api.domain.enums.StatusAgendamento;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AgendamentoRequest(
        @NotNull(message = "Cliente e obrigatorio")
        Long clienteId,
        @NotNull(message = "Servico e obrigatorio")
        Long servicoId,
        Long veiculoId,
        @NotNull(message = "Data e obrigatoria")
        @FutureOrPresent(message = "Data deve ser hoje ou futura")
        LocalDate data,
        @NotNull(message = "Horario e obrigatorio")
        LocalTime horario,
        StatusAgendamento status,
        @Size(max = 500, message = "Observacoes deve ter ate 500 caracteres")
        String observacoes) {
}
