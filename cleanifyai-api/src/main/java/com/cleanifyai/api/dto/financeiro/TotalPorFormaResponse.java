package com.cleanifyai.api.dto.financeiro;

import java.math.BigDecimal;

import com.cleanifyai.api.domain.enums.FormaPagamento;

public record TotalPorFormaResponse(
        FormaPagamento formaPagamento,
        BigDecimal entradas,
        BigDecimal saidas) {
}
