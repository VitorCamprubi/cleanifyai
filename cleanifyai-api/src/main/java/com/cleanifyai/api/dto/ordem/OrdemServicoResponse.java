package com.cleanifyai.api.dto.ordem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.cleanifyai.api.domain.enums.StatusOrdem;

public record OrdemServicoResponse(
        Long id,
        Long clienteId,
        String clienteNome,
        Long veiculoId,
        String veiculoDescricao,
        String veiculoPlaca,
        Long agendamentoId,
        StatusOrdem status,
        BigDecimal valorTotal,
        Instant abertaEm,
        Instant fechadaEm,
        String observacoes,
        List<ItemOrdemResponse> itens) {
}
