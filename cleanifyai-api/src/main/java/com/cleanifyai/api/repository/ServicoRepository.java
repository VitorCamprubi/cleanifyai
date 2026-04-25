package com.cleanifyai.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cleanifyai.api.domain.entity.Servico;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findAllByEmpresaId(Long empresaId, Sort sort);

    Optional<Servico> findByIdAndEmpresaId(Long id, Long empresaId);

    long countByEmpresaIdAndAtivoTrue(Long empresaId);
}
