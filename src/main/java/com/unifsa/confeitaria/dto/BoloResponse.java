package com.unifsa.confeitaria.dto;

import com.unifsa.confeitaria.entity.Bolo;
import java.math.BigDecimal;

public record BoloResponse(Long id, String nome, String descricao, String sabor,
                           BigDecimal preco, Integer estoque, Boolean ativo) {

    public static BoloResponse de(Bolo b) {
        return new BoloResponse(b.getId(), b.getNome(), b.getDescricao(), b.getSabor(),
                b.getPreco(), b.getEstoque(), b.getAtivo());
    }
}
