package com.cleanifyai.api.dto.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumoFinanceiroResponse(
        LocalDate inicio,
        LocalDate fim,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal saldo,
        long quantidadeLancamentos,
        List<TotalPorFormaResponse> porForma) {
}
