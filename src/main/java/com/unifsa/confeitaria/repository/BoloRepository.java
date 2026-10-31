package com.unifsa.confeitaria.repository;

import com.unifsa.confeitaria.entity.Bolo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BoloRepository extends JpaRepository<Bolo, Long>, JpaSpecificationExecutor<Bolo> {
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
