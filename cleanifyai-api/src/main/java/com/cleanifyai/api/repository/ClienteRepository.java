package com.cleanifyai.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cleanifyai.api.domain.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findAllByEmpresaIdAndAtivoTrue(Long empresaId, Sort sort);

    Optional<Cliente> findByIdAndEmpresaIdAndAtivoTrue(Long id, Long empresaId);

    Optional<Cliente> findByIdAndEmpresaId(Long id, Long empresaId);

    long countByEmpresaIdAndAtivoTrue(Long empresaId);
}
