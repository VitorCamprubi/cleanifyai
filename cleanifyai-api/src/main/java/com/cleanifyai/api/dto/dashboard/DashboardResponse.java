package com.cleanifyai.api.dto.dashboard;

import java.util.List;

public record DashboardResponse(
        long totalClientes,
        long totalServicosAtivos,
        long totalAgendamentosDoDia,
        List<ProximoAgendamentoResponse> proximosAgendamentos) {
}

