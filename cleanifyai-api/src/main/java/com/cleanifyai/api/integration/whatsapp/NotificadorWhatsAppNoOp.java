package com.cleanifyai.api.integration.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cleanifyai.api.domain.entity.Agendamento;

@Component
public class NotificadorWhatsAppNoOp implements NotificadorWhatsApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificadorWhatsAppNoOp.class);

    @Override
    public void notificarAgendamentoCriado(Agendamento agendamento) {
        logAcao("criacao", agendamento);
    }

    @Override
    public void notificarAgendamentoAtualizado(Agendamento agendamento) {
        logAcao("atualizacao", agendamento);
    }

    @Override
    public void notificarStatusAlterado(Agendamento agendamento) {
        logAcao("status", agendamento);
    }

    @Override
    public void notificarAgendamentoCancelado(Agendamento agendamento) {
        logAcao("cancelamento", agendamento);
    }

    private void logAcao(String acao, Agendamento agendamento) {
        // Placeholder intencional: a integracao real devera ser implementada aqui ou via evento assincrono.
        LOGGER.debug("Integracao WhatsApp desabilitada para {} do agendamento {}", acao, agendamento.getId());
    }
}

