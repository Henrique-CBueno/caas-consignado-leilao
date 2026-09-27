package com.caas.tenant.infrastructure;

import com.caas.tenant.domain.TenantId;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantId> CURRENT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantId tenantId) {
        CURRENT.set(tenantId);
    }

    public static TenantId get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
