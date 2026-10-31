package com.unifsa.confeitaria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres") String nome,
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido") String email,
        @Pattern(regexp = "\\d{10,11}", message = "O telefone deve ter 10 ou 11 dígitos (somente números)") String telefone) {
}
