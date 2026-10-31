package com.unifsa.confeitaria.controller;

import com.unifsa.confeitaria.dto.BoloRequest;
import com.unifsa.confeitaria.dto.BoloResponse;
import com.unifsa.confeitaria.entity.Bolo;
import com.unifsa.confeitaria.service.BoloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/bolos")
@Tag(name = "Bolos", description = "CRUD do cardápio de bolos")
public class BoloController {

    private final BoloService service;

    public BoloController(BoloService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastrar bolo (201; 400 se inválido; 409 se nome já existe)")
    @PostMapping
    public ResponseEntity<BoloResponse> criar(@Valid @RequestBody BoloRequest request) {
        Bolo salvo = service.criar(request);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(salvo.getId()).toUri();
        return ResponseEntity.created(local).body(BoloResponse.de(salvo));
    }

    @Operation(summary = "Listar bolos, com filtros por nome, sabor, faixa de preço e ativo")
    @GetMapping
    public ResponseEntity<List<BoloResponse>> listar(@RequestParam(required = false) String nome,
                                                     @RequestParam(required = false) String sabor,
                                                     @RequestParam(required = false) BigDecimal precoMin,
                                                     @RequestParam(required = false) BigDecimal precoMax,
                                                     @RequestParam(required = false) Boolean ativo) {
        List<BoloResponse> lista = service.listar(nome, sabor, precoMin, precoMax, ativo)
                .stream().map(BoloResponse::de).toList();
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Buscar bolo por id (404 se não existir)")
    @GetMapping("/{id}")
    public ResponseEntity<BoloResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(BoloResponse.de(service.buscarPorId(id)));
    }

    @Operation(summary = "Atualizar bolo")
    @PutMapping("/{id}")
    public ResponseEntity<BoloResponse> atualizar(@PathVariable Long id,
                                                  @Valid @RequestBody BoloRequest request) {
        return ResponseEntity.ok(BoloResponse.de(service.atualizar(id, request)));
    }

    @Operation(summary = "Excluir bolo (204; 409 se já foi pedido)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
