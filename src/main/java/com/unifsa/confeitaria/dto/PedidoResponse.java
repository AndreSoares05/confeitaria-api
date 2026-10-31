package com.unifsa.confeitaria.dto;

import com.unifsa.confeitaria.entity.Pedido;
import com.unifsa.confeitaria.entity.StatusPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PedidoResponse(
        Long id,
        Long clienteId,
        String clienteNome,
        Long boloId,
        String boloNome,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal descontoPercentual,
        BigDecimal valorTotal,
        LocalDate dataEntrega,
        LocalDateTime dataCriacao,
        StatusPedido status) {

    public static PedidoResponse de(Pedido p) {
        return new PedidoResponse(
                p.getId(),
                p.getCliente().getId(), p.getCliente().getNome(),
                p.getBolo().getId(), p.getBolo().getNome(),
                p.getQuantidade(), p.getPrecoUnitario(), p.getDescontoPercentual(),
                p.getValorTotal(), p.getDataEntrega(), p.getDataCriacao(), p.getStatus());
    }
}
