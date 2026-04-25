package com.cleanifyai.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cleanifyai.api.domain.entity.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);
}
