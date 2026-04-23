package com.cleanifyai.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cleanifyai.api.domain.entity.Servico;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    long countByAtivoTrue();
}

