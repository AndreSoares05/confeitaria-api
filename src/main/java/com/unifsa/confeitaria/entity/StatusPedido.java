package com.unifsa.confeitaria.entity;

import com.unifsa.confeitaria.exception.RegraNegocioException;

public enum StatusPedido {
    PENDENTE, CONFIRMADO, EM_PRODUCAO, ENTREGUE, CANCELADO;

    
    public StatusPedido proximo() {
        return switch (this) {
            case PENDENTE -> CONFIRMADO;
            case CONFIRMADO -> EM_PRODUCAO;
            case EM_PRODUCAO -> ENTREGUE;
            default -> throw new RegraNegocioException(
                    "Pedido com status " + this + " não pode avançar para outra etapa.");
        };
    }

    public boolean podeCancelar() {
        return this == PENDENTE || this == CONFIRMADO;
    }
}
