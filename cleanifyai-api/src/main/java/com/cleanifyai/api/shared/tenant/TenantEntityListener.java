package com.cleanifyai.api.shared.tenant;

import com.cleanifyai.api.domain.entity.EntidadeTenantBase;

import jakarta.persistence.PrePersist;

/**
 * Injeta automaticamente o {@code empresaId} do {@link TenantContext} em qualquer entidade
 * que estenda {@link EntidadeTenantBase} no momento da persistencia.
 *
 * Garantia minima de isolamento contra esquecimento manual.
 */
public class TenantEntityListener {

    @PrePersist
    public void aplicarEmpresaIdSeNecessario(EntidadeTenantBase entidade) {
        if (entidade.getEmpresaId() != null) {
            return;
        }

        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            // Nao falha aqui para nao quebrar contextos administrativos (ex: seed inicial).
            // Servicos publicos devem chamar TenantContext.requireEmpresaId() explicitamente.
            return;
        }

        entidade.setEmpresaId(empresaId);
    }
}
