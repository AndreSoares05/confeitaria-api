package com.unifsa.confeitaria.repository;

import com.unifsa.confeitaria.entity.Pedido;
import com.unifsa.confeitaria.entity.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {
    boolean existsByClienteId(Long clienteId);
    boolean existsByBoloId(Long boloId);
    long countByClienteIdAndStatus(Long clienteId, StatusPedido status);
}
