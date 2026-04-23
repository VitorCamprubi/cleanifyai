package com.cleanifyai.api.dto.servico;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ServicoRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 120, message = "Nome deve ter ate 120 caracteres")
        String nome,
        @Size(max = 500, message = "Descricao deve ter ate 500 caracteres")
        String descricao,
        @NotNull(message = "Preco e obrigatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "Preco deve ser maior que zero")
        BigDecimal preco,
        @NotNull(message = "Duracao e obrigatoria")
        @Positive(message = "Duracao deve ser maior que zero")
        Integer duracaoMinutos,
        @NotNull(message = "Ativo e obrigatorio")
        Boolean ativo) {
}

