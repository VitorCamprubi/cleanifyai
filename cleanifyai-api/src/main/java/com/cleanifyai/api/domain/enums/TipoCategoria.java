package com.cleanifyai.api.domain.enums;

public enum TipoCategoria {
    RECEITA,
    DESPESA,
    AMBOS;

    public boolean aceitaTipoLancamento(TipoLancamento tipo) {
        if (this == AMBOS) {
            return true;
        }
        return (this == RECEITA && tipo == TipoLancamento.ENTRADA)
                || (this == DESPESA && tipo == TipoLancamento.SAIDA);
    }
}
