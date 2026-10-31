package com.unifsa.confeitaria.controller;

import com.unifsa.confeitaria.dto.ClienteRequest;
import com.unifsa.confeitaria.dto.ClienteResponse;
import com.unifsa.confeitaria.entity.Cliente;
import com.unifsa.confeitaria.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "CRUD de clientes da confeitaria")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastrar cliente (201; 400 se inválido; 409 se e-mail já existe)")
    @PostMapping
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        Cliente salvo = service.criar(request);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(salvo.getId()).toUri();
        return ResponseEntity.created(local).body(ClienteResponse.de(salvo));
    }

    @Operation(summary = "Listar clientes, com filtros opcionais por nome e e-mail")
    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar(@RequestParam(required = false) String nome,
                                                        @RequestParam(required = false) String email) {
        List<ClienteResponse> lista = service.listar(nome, email).stream().map(ClienteResponse::de).toList();
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Buscar cliente por id (404 se não existir)")
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ClienteResponse.de(service.buscarPorId(id)));
    }

    @Operation(summary = "Atualizar cliente")
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(ClienteResponse.de(service.atualizar(id, request)));
    }

    @Operation(summary = "Excluir cliente (204; 409 se tiver pedidos)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
