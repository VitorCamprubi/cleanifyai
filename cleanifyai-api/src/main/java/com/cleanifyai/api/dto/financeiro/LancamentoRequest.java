package com.cleanifyai.api.dto.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cleanifyai.api.domain.enums.FormaPagamento;
import com.cleanifyai.api.domain.enums.TipoLancamento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LancamentoRequest(
        @NotNull(message = "Tipo e obrigatorio")
        TipoLancamento tipo,
        @NotNull(message = "Valor e obrigatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "Valor deve ser maior que zero")
        BigDecimal valor,
        @NotNull(message = "Forma de pagamento e obrigatoria")
        FormaPagamento formaPagamento,
        @NotNull(message = "Data e obrigatoria")
        LocalDate dataLancamento,
        @NotBlank(message = "Descricao e obrigatoria")
        @Size(max = 200)
        String descricao,
        Long ordemId) {
}
