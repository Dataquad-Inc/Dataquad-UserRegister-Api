package com.dataquadinc.tenant;

/**
 * Holds the current request's tenant id (e.g. mymulya, aventra).
 * Set by {@link TenantInterceptor}; cleared after the request.
 */
public final class TenantContext {

    public static final String DEFAULT_TENANT = "mymulya";
    public static final String HEADER_NAME = "X-Tenant-Id";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String getTenantId() {
        String id = CURRENT.get();
        return (id == null || id.isBlank()) ? DEFAULT_TENANT : id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
