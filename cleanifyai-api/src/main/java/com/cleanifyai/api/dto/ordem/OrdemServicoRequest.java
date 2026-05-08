package com.cleanifyai.api.dto.ordem;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrdemServicoRequest(
        @NotNull(message = "Cliente e obrigatorio")
        Long clienteId,
        @NotNull(message = "Veiculo e obrigatorio")
        Long veiculoId,
        Long agendamentoId,
        @Size(max = 500)
        String observacoes,
        @Valid
        List<ItemOrdemRequest> itens) {
}
