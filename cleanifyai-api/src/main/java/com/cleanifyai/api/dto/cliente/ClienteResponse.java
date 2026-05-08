package com.cleanifyai.api.dto.cliente;

public record ClienteResponse(
        Long id,
        String nome,
        String telefone,
        String email,
        String observacoes) {
}

