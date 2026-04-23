package com.cleanifyai.api.dto.agendamento;

public record ClienteResumoResponse(
        Long id,
        String nome,
        String telefone,
        String veiculo,
        String placa) {
}

