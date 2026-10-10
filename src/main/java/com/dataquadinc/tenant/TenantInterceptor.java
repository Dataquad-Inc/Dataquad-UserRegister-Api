package com.dataquadinc.tenant;

import com.dataquadinc.model.Tenant;
import com.dataquadinc.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Resolves tenant for data isolation (not branding).
 * Order: X-Tenant-Id → ?tenant= → Host (aventrainc.ai / aventra.*) → default mymulya.
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantRepository tenantRepository;

    public TenantInterceptor(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TenantContext.setTenantId(resolveTenantId(request));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }

    private String resolveTenantId(HttpServletRequest request) {
        String fromHeader = request.getHeader(TenantContext.HEADER_NAME);
        if (isPresent(fromHeader)) {
            return normalize(fromHeader);
        }

        String fromQuery = request.getParameter("tenant");
        if (isPresent(fromQuery)) {
            return normalize(fromQuery);
        }

        String fromHost = TenantResolver.fromHost(request.getServerName());
        if (fromHost != null) {
            return normalize(fromHost);
        }

        return TenantContext.DEFAULT_TENANT;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String raw) {
        String code = raw.trim().toLowerCase();
        return tenantRepository.findByCodeAndStatus(code, "ACTIVE")
                .map(Tenant::getId)
                .orElseGet(() -> tenantRepository.findById(code)
                        .filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
                        .map(Tenant::getId)
                        .orElse(TenantContext.DEFAULT_TENANT));
    }
}
