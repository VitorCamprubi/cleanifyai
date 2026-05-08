package com.cleanifyai.api.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cleanifyai.api.domain.entity.Lancamento;
import com.cleanifyai.api.domain.enums.TipoLancamento;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    List<Lancamento> findAllByEmpresaIdAndDataLancamentoBetween(
            Long empresaId,
            LocalDate inicio,
            LocalDate fim,
            Sort sort);

    Optional<Lancamento> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmpresaIdAndOrdemIdAndTipo(Long empresaId, Long ordemId, TipoLancamento tipo);
}
