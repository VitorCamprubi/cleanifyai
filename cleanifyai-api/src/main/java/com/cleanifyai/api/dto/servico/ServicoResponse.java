package com.cleanifyai.api.dto.servico;

import java.math.BigDecimal;

public record ServicoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer duracaoMinutos,
        Boolean ativo) {
}

