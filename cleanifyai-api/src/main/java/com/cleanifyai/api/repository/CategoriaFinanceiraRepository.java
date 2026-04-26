package com.cleanifyai.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cleanifyai.api.domain.entity.CategoriaFinanceira;
import com.cleanifyai.api.domain.enums.TipoCategoria;

public interface CategoriaFinanceiraRepository extends JpaRepository<CategoriaFinanceira, Long> {

    List<CategoriaFinanceira> findAllByEmpresaIdAndAtivoTrue(Long empresaId, Sort sort);

    List<CategoriaFinanceira> findAllByEmpresaIdAndTipoInAndAtivoTrue(Long empresaId, List<TipoCategoria> tipos, Sort sort);

    Optional<CategoriaFinanceira> findByIdAndEmpresaIdAndAtivoTrue(Long id, Long empresaId);

    Optional<CategoriaFinanceira> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmpresaIdAndNomeAndAtivoTrue(Long empresaId, String nome);
}
