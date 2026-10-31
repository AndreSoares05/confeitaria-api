package com.unifsa.confeitaria.service;

import com.unifsa.confeitaria.dto.PedidoRequest;
import com.unifsa.confeitaria.dto.PedidoResponse;
import com.unifsa.confeitaria.dto.PedidoUpdateRequest;
import com.unifsa.confeitaria.entity.Bolo;
import com.unifsa.confeitaria.entity.Cliente;
import com.unifsa.confeitaria.entity.Pedido;
import com.unifsa.confeitaria.entity.StatusPedido;
import com.unifsa.confeitaria.exception.RecursoNaoEncontradoException;
import com.unifsa.confeitaria.exception.RegraNegocioException;
import com.unifsa.confeitaria.repository.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    /** Antecedência mínima (em dias) entre o pedido e a entrega. */
    private static final int PRAZO_MINIMO_DIAS = 2;
    /** Máximo de pedidos PENDENTES por cliente. */
    private static final int MAX_PENDENTES_POR_CLIENTE = 3;

    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final BoloService boloService;

    public PedidoService(PedidoRepository pedidoRepository, ClienteService clienteService, BoloService boloService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteService = clienteService;
        this.boloService = boloService;
    }

    @Transactional
    public PedidoResponse criar(PedidoRequest req) {
        Cliente cliente = clienteService.buscarPorId(req.clienteId());
        Bolo bolo = boloService.buscarPorId(req.boloId());

        // Regra 1: bolo precisa estar ativo no cardápio
        if (!Boolean.TRUE.equals(bolo.getAtivo())) {
            throw new RegraNegocioException("O bolo '" + bolo.getNome() + "' está inativo e não pode ser pedido.");
        }
        // Regra 2: prazo mínimo de entrega
        validarDataEntrega(req.dataEntrega());
        // Regra 3: limite de pedidos pendentes por cliente
        long pendentes = pedidoRepository.countByClienteIdAndStatus(cliente.getId(), StatusPedido.PENDENTE);
        if (pendentes >= MAX_PENDENTES_POR_CLIENTE) {
            throw new RegraNegocioException("O cliente já possui " + MAX_PENDENTES_POR_CLIENTE
                    + " pedidos pendentes. Conclua ou cancele algum antes de fazer outro.");
        }
        // Regra 4: estoque suficiente (e baixa do estoque)
        reservarEstoque(bolo, req.quantidade());

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setBolo(bolo);
        pedido.setQuantidade(req.quantidade());
        pedido.setPrecoUnitario(bolo.getPreco()); // guarda o preço da época do pedido
        pedido.setDataEntrega(req.dataEntrega());
        pedido.setStatus(StatusPedido.PENDENTE);
        // Regra 5: desconto por quantidade
        calcularValores(pedido);

        return PedidoResponse.de(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(StatusPedido status, Long clienteId, Long boloId,
                                       LocalDate entregaDe, LocalDate entregaAte) {
        Specification<Pedido> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (clienteId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("cliente").get("id"), clienteId));
        }
        if (boloId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("bolo").get("id"), boloId));
        }
        if (entregaDe != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("dataEntrega"), entregaDe));
        }
        if (entregaAte != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("dataEntrega"), entregaAte));
        }
        return pedidoRepository.findAll(spec, Sort.by("id")).stream().map(PedidoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {
        return PedidoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public PedidoResponse atualizar(Long id, PedidoUpdateRequest req) {
        Pedido pedido = buscarEntidade(id);
        // Regra 6: só dá para editar enquanto estiver PENDENTE
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new RegraNegocioException("Só é possível editar pedidos PENDENTES. Status atual: " + pedido.getStatus());
        }
        validarDataEntrega(req.dataEntrega());

        Bolo bolo = pedido.getBolo();
        bolo.setEstoque(bolo.getEstoque() + pedido.getQuantidade()); // devolve a quantidade antiga
        reservarEstoque(bolo, req.quantidade());                      // reserva a nova (falha => rollback)

        pedido.setQuantidade(req.quantidade());
        pedido.setDataEntrega(req.dataEntrega());
        calcularValores(pedido);
        return PedidoResponse.de(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse avancarStatus(Long id) {
        Pedido pedido = buscarEntidade(id);
        pedido.setStatus(pedido.getStatus().proximo());
        return PedidoResponse.de(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse cancelar(Long id) {
        Pedido pedido = buscarEntidade(id);
        // Regra 7: só cancela PENDENTE ou CONFIRMADO; estoque volta para o bolo
        if (!pedido.getStatus().podeCancelar()) {
            throw new RegraNegocioException("Pedido com status " + pedido.getStatus() + " não pode ser cancelado.");
        }
        Bolo bolo = pedido.getBolo();
        bolo.setEstoque(bolo.getEstoque() + pedido.getQuantidade());
        pedido.setStatus(StatusPedido.CANCELADO);
        return PedidoResponse.de(pedidoRepository.save(pedido));
    }

    @Transactional
    public void excluir(Long id) {
        Pedido pedido = buscarEntidade(id);
        // Regra 8: só exclui pedido CANCELADO (histórico de vendas é preservado)
        if (pedido.getStatus() != StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Só é possível excluir pedidos CANCELADOS. Cancele o pedido primeiro.");
        }
        pedidoRepository.delete(pedido);
    }

    // ---------- métodos auxiliares ----------

    private Pedido buscarEntidade(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido com id " + id + " não encontrado."));
    }

    private void validarDataEntrega(LocalDate dataEntrega) {
        LocalDate minima = LocalDate.now().plusDays(PRAZO_MINIMO_DIAS);
        if (dataEntrega.isBefore(minima)) {
            throw new RegraNegocioException("A entrega deve ser agendada com pelo menos " + PRAZO_MINIMO_DIAS
                    + " dias de antecedência (a partir de " + minima + ").");
        }
    }

    private void reservarEstoque(Bolo bolo, int quantidade) {
        if (bolo.getEstoque() < quantidade) {
            throw new RegraNegocioException("Estoque insuficiente para '" + bolo.getNome()
                    + "': disponível " + bolo.getEstoque() + ", solicitado " + quantidade + ".");
        }
        bolo.setEstoque(bolo.getEstoque() - quantidade);
    }

    private BigDecimal percentualDesconto(int quantidade) {
        if (quantidade >= 10) return BigDecimal.valueOf(10);
        if (quantidade >= 5) return BigDecimal.valueOf(5);
        return BigDecimal.ZERO;
    }

    private void calcularValores(Pedido pedido) {
        BigDecimal bruto = pedido.getPrecoUnitario().multiply(BigDecimal.valueOf(pedido.getQuantidade()));
        BigDecimal percentual = percentualDesconto(pedido.getQuantidade());
        BigDecimal desconto = bruto.multiply(percentual).divide(BigDecimal.valueOf(100));
        pedido.setDescontoPercentual(percentual);
        pedido.setValorTotal(bruto.subtract(desconto).setScale(2, RoundingMode.HALF_UP));
    }
}
