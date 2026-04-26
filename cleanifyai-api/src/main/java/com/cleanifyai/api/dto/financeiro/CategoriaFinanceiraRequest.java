package com.cleanifyai.api.dto.financeiro;

import com.cleanifyai.api.domain.enums.TipoCategoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoriaFinanceiraRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 80)
        String nome,
        @NotNull(message = "Tipo e obrigatorio")
        TipoCategoria tipo,
        @Pattern(regexp = "^#?[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$", message = "Cor deve estar em hexadecimal, ex: #18E4D3")
        String cor) {
}
