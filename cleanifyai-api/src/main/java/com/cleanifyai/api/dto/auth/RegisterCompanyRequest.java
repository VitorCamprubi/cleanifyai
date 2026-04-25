package com.cleanifyai.api.dto.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterCompanyRequest(
        @Valid
        EmpresaPayload empresa,
        @Valid
        AdminPayload admin) {

    public record EmpresaPayload(
            @NotBlank(message = "Nome da empresa e obrigatorio")
            @Size(max = 160)
            String nome,
            @Size(max = 18)
            String cnpj,
            @Size(max = 20)
            String telefone,
            @Email(message = "Email invalido")
            @Size(max = 160)
            String email) {
    }

    public record AdminPayload(
            @NotBlank(message = "Nome do administrador e obrigatorio")
            @Size(max = 120)
            String nome,
            @NotBlank(message = "Email e obrigatorio")
            @Email(message = "Email invalido")
            @Size(max = 160)
            String email,
            @NotBlank(message = "Senha e obrigatoria")
            @Size(min = 6, max = 64, message = "Senha deve ter entre 6 e 64 caracteres")
            String senha) {
    }
}
