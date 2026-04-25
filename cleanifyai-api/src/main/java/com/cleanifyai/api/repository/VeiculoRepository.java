package com.cleanifyai.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cleanifyai.api.domain.entity.Veiculo;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    List<Veiculo> findAllByEmpresaIdAndAtivoTrue(Long empresaId, Sort sort);

    List<Veiculo> findAllByEmpresaIdAndClienteIdAndAtivoTrue(Long empresaId, Long clienteId, Sort sort);

    Optional<Veiculo> findByIdAndEmpresaIdAndAtivoTrue(Long id, Long empresaId);

    Optional<Veiculo> findByIdAndEmpresaId(Long id, Long empresaId);
}
