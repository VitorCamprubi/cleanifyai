package com.cleanifyai.api.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cleanifyai.api.domain.entity.Agendamento;
import com.cleanifyai.api.domain.enums.StatusAgendamento;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    long countByData(LocalDate data);

    boolean existsByClienteId(Long clienteId);

    boolean existsByServicoId(Long servicoId);

    List<Agendamento> findTop10ByStatusInAndDataGreaterThanEqualOrderByDataAscHorarioAsc(
            Collection<StatusAgendamento> status,
            LocalDate data);
}

