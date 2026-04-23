package com.cleanifyai.api.integration.ai;

import com.cleanifyai.api.domain.entity.Agendamento;

public interface MotorSugestaoIa {

    String gerarResumoAtendimento(Agendamento agendamento);
}

