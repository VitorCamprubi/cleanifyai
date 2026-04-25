package com.cleanifyai.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cleanifyai.api.domain.entity.OrdemServico;
import com.cleanifyai.api.domain.enums.StatusOrdem;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    List<OrdemServico> findAllByEmpresaId(Long empresaId, Sort sort);

    List<OrdemServico> findAllByEmpresaIdAndStatus(Long empresaId, StatusOrdem status, Sort sort);

    Optional<OrdemServico> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmpresaIdAndAgendamentoId(Long empresaId, Long agendamentoId);
}
