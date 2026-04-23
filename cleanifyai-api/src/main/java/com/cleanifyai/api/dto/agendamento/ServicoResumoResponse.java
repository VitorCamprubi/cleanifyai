package com.cleanifyai.api.dto.agendamento;

import java.math.BigDecimal;

public record ServicoResumoResponse(
        Long id,
        String nome,
        BigDecimal preco,
        Integer duracaoMinutos) {
}

