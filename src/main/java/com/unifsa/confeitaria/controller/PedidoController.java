package com.unifsa.confeitaria.controller;

import com.unifsa.confeitaria.dto.PedidoRequest;
import com.unifsa.confeitaria.dto.PedidoResponse;
import com.unifsa.confeitaria.dto.PedidoUpdateRequest;
import com.unifsa.confeitaria.entity.StatusPedido;
import com.unifsa.confeitaria.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos", description = "Pedidos de bolos (relaciona Cliente e Bolo) com regras de negócio")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @Operation(summary = "Criar pedido (201; 404 cliente/bolo; 422 regra de negócio)")
    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest request) {
        PedidoResponse salvo = service.criar(request);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(salvo.id()).toUri();
        return ResponseEntity.created(local).body(salvo);
    }

    @Operation(summary = "Listar pedidos, com filtros por status, cliente, bolo e período de entrega")
    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listar(
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long boloId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entregaDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entregaAte) {
        return ResponseEntity.ok(service.listar(status, clienteId, boloId, entregaDe, entregaAte));
    }

    @Operation(summary = "Buscar pedido por id (404 se não existir)")
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Atualizar quantidade e data de entrega (somente pedido PENDENTE)")
    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponse> atualizar(@PathVariable Long id,
                                                    @Valid @RequestBody PedidoUpdateRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @Operation(summary = "Avançar status: PENDENTE > CONFIRMADO > EM_PRODUCAO > ENTREGUE")
    @PatchMapping("/{id}/avancar")
    public ResponseEntity<PedidoResponse> avancar(@PathVariable Long id) {
        return ResponseEntity.ok(service.avancarStatus(id));
    }

    @Operation(summary = "Cancelar pedido (PENDENTE ou CONFIRMADO) e devolver o estoque")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelar(id));
    }

    @Operation(summary = "Excluir pedido (204; somente CANCELADO)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
