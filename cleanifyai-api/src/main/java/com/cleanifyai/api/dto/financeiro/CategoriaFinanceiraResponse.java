package com.cleanifyai.api.dto.financeiro;

import com.cleanifyai.api.domain.enums.TipoCategoria;

public record CategoriaFinanceiraResponse(
        Long id,
        String nome,
        TipoCategoria tipo,
        String cor) {
}
