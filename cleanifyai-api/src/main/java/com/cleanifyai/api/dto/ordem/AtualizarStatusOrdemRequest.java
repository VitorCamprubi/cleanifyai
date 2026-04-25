package com.cleanifyai.api.dto.ordem;

import com.cleanifyai.api.domain.enums.StatusOrdem;

import jakarta.validation.constraints.NotNull;

public record AtualizarStatusOrdemRequest(
        @NotNull(message = "Status e obrigatorio")
        StatusOrdem status) {
}
