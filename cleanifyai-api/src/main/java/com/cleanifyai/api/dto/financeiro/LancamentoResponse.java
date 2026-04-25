package com.cleanifyai.api.dto.financeiro;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cleanifyai.api.domain.enums.FormaPagamento;
import com.cleanifyai.api.domain.enums.TipoLancamento;

public record LancamentoResponse(
        Long id,
        TipoLancamento tipo,
        BigDecimal valor,
        FormaPagamento formaPagamento,
        LocalDate dataLancamento,
        String descricao,
        Long ordemId,
        Instant registradoEm) {
}
