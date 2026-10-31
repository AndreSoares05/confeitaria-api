package com.unifsa.confeitaria.service;

import com.unifsa.confeitaria.dto.ClienteRequest;
import com.unifsa.confeitaria.entity.Cliente;
import com.unifsa.confeitaria.exception.ConflitoException;
import com.unifsa.confeitaria.exception.RecursoNaoEncontradoException;
import com.unifsa.confeitaria.repository.ClienteRepository;
import com.unifsa.confeitaria.repository.PedidoRepository;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    public ClienteService(ClienteRepository clienteRepository, PedidoRepository pedidoRepository) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public Cliente criar(ClienteRequest req) {
        Cliente cliente = new Cliente();
        cliente.setNome(req.nome());
        cliente.setEmail(req.email().trim().toLowerCase());
        cliente.setTelefone(req.telefone());
        // Regra: e-mail único
        if (clienteRepository.existsByEmailIgnoreCase(cliente.getEmail())) {
            throw new ConflitoException("Já existe um cliente cadastrado com o e-mail " + cliente.getEmail());
        }
        return clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar(String nome, String email) {
        Specification<Cliente> spec = (root, query, cb) -> cb.conjunction();
        if (nome != null && !nome.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("nome")), "%" + nome.trim().toLowerCase() + "%"));
        }
        if (email != null && !email.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + email.trim().toLowerCase() + "%"));
        }
        return clienteRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente com id " + id + " não encontrado."));
    }

    @Transactional
    public Cliente atualizar(Long id, ClienteRequest dados) {
        Cliente existente = buscarPorId(id);
        String email = dados.email().trim().toLowerCase();
        if (clienteRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflitoException("O e-mail " + email + " já pertence a outro cliente.");
        }
        existente.setNome(dados.nome());
        existente.setEmail(email);
        existente.setTelefone(dados.telefone());
        return clienteRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        Cliente cliente = buscarPorId(id);
        // Regra: não excluir cliente que possui pedidos
        if (pedidoRepository.existsByClienteId(id)) {
            throw new ConflitoException("Não é possível excluir: o cliente possui pedidos cadastrados.");
        }
        clienteRepository.delete(cliente);
    }
}
