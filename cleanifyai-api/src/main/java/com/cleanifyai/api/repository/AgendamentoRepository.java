package com.cleanifyai.api.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cleanifyai.api.domain.entity.Agendamento;
import com.cleanifyai.api.domain.enums.StatusAgendamento;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findAllByEmpresaId(Long empresaId, Sort sort);

    Optional<Agendamento> findByIdAndEmpresaId(Long id, Long empresaId);

    long countByEmpresaIdAndData(Long empresaId, LocalDate data);

    boolean existsByEmpresaIdAndClienteId(Long empresaId, Long clienteId);

    boolean existsByEmpresaIdAndServicoId(Long empresaId, Long servicoId);

    List<Agendamento> findTop10ByEmpresaIdAndStatusInAndDataGreaterThanEqualOrderByDataAscHorarioAsc(
            Long empresaId,
            Collection<StatusAgendamento> status,
            LocalDate data);
}
