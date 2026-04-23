package com.cleanifyai.api.integration.ai;

import org.springframework.stereotype.Component;

import com.cleanifyai.api.domain.entity.Agendamento;

@Component
public class MotorSugestaoIaNoOp implements MotorSugestaoIa {

    @Override
    public String gerarResumoAtendimento(Agendamento agendamento) {
        return "IA desabilitada no MVP";
    }
}

