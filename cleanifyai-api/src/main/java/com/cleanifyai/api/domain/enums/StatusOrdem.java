package com.cleanifyai.api.domain.enums;

public enum StatusOrdem {
    ABERTA,
    EM_EXECUCAO,
    CONCLUIDA,
    ENTREGUE,
    CANCELADA;

    public boolean ehFinal() {
        return this == ENTREGUE || this == CANCELADA;
    }

    public boolean permiteEdicaoDeItens() {
        return this == ABERTA || this == EM_EXECUCAO;
    }
}
