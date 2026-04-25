package com.cleanifyai.api.dto.veiculo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VeiculoRequest(
        @NotNull(message = "Cliente e obrigatorio")
        Long clienteId,
        @NotBlank(message = "Marca e obrigatoria")
        @Size(max = 60)
        String marca,
        @NotBlank(message = "Modelo e obrigatorio")
        @Size(max = 80)
        String modelo,
        @Size(max = 10)
        String placa,
        @Size(max = 30)
        String cor,
        @Min(value = 1900, message = "Ano invalido")
        Integer anoModelo,
        @Size(max = 500)
        String observacoes) {
}
