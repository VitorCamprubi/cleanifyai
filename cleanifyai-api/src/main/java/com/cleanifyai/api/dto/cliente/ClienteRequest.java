package com.cleanifyai.api.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 120, message = "Nome deve ter ate 120 caracteres")
        String nome,
        @NotBlank(message = "Telefone e obrigatorio")
        @Pattern(regexp = "^[+0-9()\\-\\s]{10,20}$", message = "Telefone invalido")
        @Size(max = 20, message = "Telefone deve ter ate 20 caracteres")
        String telefone,
        @Email(message = "Email invalido")
        @Size(max = 120, message = "Email deve ter ate 120 caracteres")
        String email,
        @Size(max = 500, message = "Observacoes deve ter ate 500 caracteres")
        String observacoes) {
}

