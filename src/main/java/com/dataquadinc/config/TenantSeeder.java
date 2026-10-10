package com.dataquadinc.config;

import com.dataquadinc.model.Tenant;
import com.dataquadinc.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TenantSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantSeeder.class);

    private final TenantRepository tenantRepository;
    private final JdbcTemplate jdbcTemplate;

    public TenantSeeder(TenantRepository tenantRepository, JdbcTemplate jdbcTemplate) {
        this.tenantRepository = tenantRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedTenant("mymulya", "MyMulya", "https://mymulya.com",
                "notifications@adroitinnovative.com", "*");
        seedTenant("aventra", "Aventra", "https://portal.aventrainc.ai",
                "notifications@adroitinnovative.com", "*");

        int updated = jdbcTemplate.update(
                "UPDATE user_details SET tenant_id = 'mymulya' WHERE tenant_id IS NULL OR tenant_id = ''");
        if (updated > 0) {
            log.info("Backfilled tenant_id=mymulya on {} user_details rows", updated);
        }
    }

    private void seedTenant(String id, String displayName, String frontendUrl,
                            String fromEmail, String enabledModules) {
        if (tenantRepository.existsById(id)) {
            return;
        }
        Tenant t = new Tenant();
        t.setId(id);
        t.setCode(id);
        t.setDisplayName(displayName);
        t.setFrontendUrl(frontendUrl);
        t.setFromEmail(fromEmail);
        t.setEnabledModules(enabledModules);
        t.setStatus("ACTIVE");
        tenantRepository.save(t);
        log.info("Seeded tenant {}", id);
    }
}
