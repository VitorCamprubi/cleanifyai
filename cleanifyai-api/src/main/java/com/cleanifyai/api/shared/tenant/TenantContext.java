package com.cleanifyai.api.shared.tenant;

/**
 * Contexto de tenant baseado em ThreadLocal.
 * Populado pelo {@code JwtAuthenticationFilter} a partir do claim {@code empresaId} do JWT.
 * Lido pelos serviços e pelo {@link TenantEntityListener} para isolar dados por empresa.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_EMPRESA = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setEmpresaId(Long empresaId) {
        CURRENT_EMPRESA.set(empresaId);
    }

    public static Long getEmpresaId() {
        return CURRENT_EMPRESA.get();
    }

    public static Long requireEmpresaId() {
        Long empresaId = CURRENT_EMPRESA.get();
        if (empresaId == null) {
            throw new IllegalStateException("TenantContext nao populado: requisicao sem empresa_id");
        }
        return empresaId;
    }

    public static void clear() {
        CURRENT_EMPRESA.remove();
    }
}
