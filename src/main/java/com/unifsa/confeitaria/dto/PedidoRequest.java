package com.unifsa.confeitaria.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PedidoRequest(
        @NotNull(message = "O clienteId é obrigatório") Long clienteId,
        @NotNull(message = "O boloId é obrigatório") Long boloId,
        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade mínima é 1")
        @Max(value = 100, message = "A quantidade máxima por pedido é 100") Integer quantidade,
        @NotNull(message = "A data de entrega é obrigatória (AAAA-MM-DD)") LocalDate dataEntrega) {
}
