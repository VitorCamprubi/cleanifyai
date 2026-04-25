package com.cleanifyai.api.dto.ordem;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemOrdemRequest(
        @NotNull(message = "Servico e obrigatorio")
        Long servicoId,
        @Size(max = 200)
        String descricao,
        @NotNull(message = "Quantidade e obrigatoria")
        @Min(value = 1, message = "Quantidade deve ser ao menos 1")
        Integer quantidade,
        @NotNull(message = "Valor unitario e obrigatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "Valor unitario deve ser maior que zero")
        BigDecimal valorUnitario) {
}
