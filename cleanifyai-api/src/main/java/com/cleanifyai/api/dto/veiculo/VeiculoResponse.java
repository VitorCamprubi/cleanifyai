package com.cleanifyai.api.dto.veiculo;

public record VeiculoResponse(
        Long id,
        Long clienteId,
        String clienteNome,
        String marca,
        String modelo,
        String placa,
        String cor,
        Integer anoModelo,
        String observacoes) {
}
