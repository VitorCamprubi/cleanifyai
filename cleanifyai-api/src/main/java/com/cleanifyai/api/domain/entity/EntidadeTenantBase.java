package com.cleanifyai.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class EntidadeTenantBase {

    @Column(name = "empresa_id")
    private Long empresaId;

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }
}

