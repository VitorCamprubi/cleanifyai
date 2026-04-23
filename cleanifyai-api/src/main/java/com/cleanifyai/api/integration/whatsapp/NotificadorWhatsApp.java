package com.cleanifyai.api.integration.whatsapp;

import com.cleanifyai.api.domain.entity.Agendamento;

public interface NotificadorWhatsApp {

    void notificarAgendamentoCriado(Agendamento agendamento);

    void notificarAgendamentoAtualizado(Agendamento agendamento);

    void notificarStatusAlterado(Agendamento agendamento);

    void notificarAgendamentoCancelado(Agendamento agendamento);
}

