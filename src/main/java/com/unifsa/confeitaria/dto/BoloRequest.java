package com.unifsa.confeitaria.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BoloRequest(
        @NotBlank(message = "O nome do bolo é obrigatório") String nome,
        String descricao,
        @NotBlank(message = "O sabor é obrigatório") String sabor,
        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero") BigDecimal preco,
        @NotNull(message = "O estoque é obrigatório")
        @Min(value = 0, message = "O estoque não pode ser negativo") Integer estoque,
        Boolean ativo) {
}
