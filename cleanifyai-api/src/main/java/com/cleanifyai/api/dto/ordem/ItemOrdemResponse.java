package com.cleanifyai.api.dto.ordem;

import java.math.BigDecimal;

public record ItemOrdemResponse(
        Long id,
        Long servicoId,
        String servicoNome,
        String descricao,
        Integer quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal) {
}
