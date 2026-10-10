package com.dataquadinc.tenant;

/**
 * Maps login identity / host to a tenant id.
 * Product branding stays MyMulya — tenant only isolates data.
 */
public final class TenantResolver {

    public static final String AVENTRA = "aventra";
    public static final String MYMULYA = TenantContext.DEFAULT_TENANT;

    private TenantResolver() {
    }

    /** e.g. user@aventrainc.ai → aventra */
    public static String fromEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return null;
        }
        String domain = email.substring(email.lastIndexOf('@') + 1).trim().toLowerCase();
        if (domain.equals("aventrainc.ai") || domain.endsWith(".aventrainc.ai")) {
            return AVENTRA;
        }
        return null;
    }

    /** e.g. portal.aventrainc.ai / aventra.mymulya.com → aventra */
    public static String fromHost(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        String h = host.trim().toLowerCase();
        if (h.equals("aventrainc.ai") || h.endsWith(".aventrainc.ai")) {
            return AVENTRA;
        }
        if (h.startsWith("aventra.")) {
            return AVENTRA;
        }
        return null;
    }

    /**
     * Prefer explicit context (header/query/host), else email domain, else default.
     */
    public static String resolveForLogin(String email, String contextTenantId) {
        String fromEmail = fromEmail(email);
        if (fromEmail != null) {
            return fromEmail;
        }
        if (contextTenantId != null && !contextTenantId.isBlank()) {
            return contextTenantId;
        }
        return MYMULYA;
    }
}
