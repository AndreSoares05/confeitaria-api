package com.unifsa.confeitaria.service;

import com.unifsa.confeitaria.dto.BoloRequest;
import com.unifsa.confeitaria.entity.Bolo;
import com.unifsa.confeitaria.exception.ConflitoException;
import com.unifsa.confeitaria.exception.RecursoNaoEncontradoException;
import com.unifsa.confeitaria.repository.BoloRepository;
import com.unifsa.confeitaria.repository.PedidoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoloService {

    private final BoloRepository boloRepository;
    private final PedidoRepository pedidoRepository;

    public BoloService(BoloRepository boloRepository, PedidoRepository pedidoRepository) {
        this.boloRepository = boloRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public Bolo criar(BoloRequest req) {
        Bolo bolo = new Bolo();
        bolo.setNome(req.nome());
        bolo.setDescricao(req.descricao());
        bolo.setSabor(req.sabor());
        bolo.setPreco(req.preco());
        bolo.setEstoque(req.estoque());
        bolo.setAtivo(req.ativo() == null ? true : req.ativo());
        // Regra: nome único
        if (boloRepository.existsByNomeIgnoreCase(bolo.getNome().trim())) {
            throw new ConflitoException("Já existe um bolo com o nome '" + bolo.getNome() + "'.");
        }
        return boloRepository.save(bolo);
    }

    @Transactional(readOnly = true)
    public List<Bolo> listar(String nome, String sabor, BigDecimal precoMin, BigDecimal precoMax, Boolean ativo) {
        Specification<Bolo> spec = (root, query, cb) -> cb.conjunction();
        if (nome != null && !nome.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("nome")), "%" + nome.trim().toLowerCase() + "%"));
        }
        if (sabor != null && !sabor.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("sabor")), "%" + sabor.trim().toLowerCase() + "%"));
        }
        if (precoMin != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("preco"), precoMin));
        }
        if (precoMax != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("preco"), precoMax));
        }
        if (ativo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ativo"), ativo));
        }
        return boloRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public Bolo buscarPorId(Long id) {
        return boloRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bolo com id " + id + " não encontrado."));
    }

    @Transactional
    public Bolo atualizar(Long id, BoloRequest dados) {
        Bolo existente = buscarPorId(id);
        if (boloRepository.existsByNomeIgnoreCaseAndIdNot(dados.nome().trim(), id)) {
            throw new ConflitoException("Já existe outro bolo com o nome '" + dados.nome() + "'.");
        }
        existente.setNome(dados.nome());
        existente.setDescricao(dados.descricao());
        existente.setSabor(dados.sabor());
        existente.setPreco(dados.preco());
        existente.setEstoque(dados.estoque());
        if (dados.ativo() != null) {
            existente.setAtivo(dados.ativo());
        }
        return boloRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        Bolo bolo = buscarPorId(id);
        // Regra: não excluir bolo que já foi pedido (use "ativo = false" para tirar do cardápio)
        if (pedidoRepository.existsByBoloId(id)) {
            throw new ConflitoException(
                    "Não é possível excluir: o bolo possui pedidos. Inative-o (ativo = false) para tirá-lo do cardápio.");
        }
        boloRepository.delete(bolo);
    }
}
