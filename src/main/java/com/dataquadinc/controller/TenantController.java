package com.dataquadinc.controller;

import com.dataquadinc.dto.TenantPublicDto;
import com.dataquadinc.model.Tenant;
import com.dataquadinc.repository.TenantRepository;
import com.dataquadinc.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users/tenants")
@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000",
        "https://mymulya.com", "http://localhost:3000", "http://192.168.0.135:8080", "http://192.168.0.135",
        "http://154.210.288.26", "http://192.168.0.203:3000", "http://192.168.0.167:3000",
        "https://portal.aventrainc.ai"})
public class TenantController {

    private final TenantRepository tenantRepository;

    public TenantController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /** Public branding/config for the current (or requested) tenant. */
    @GetMapping("/current")
    public ResponseEntity<TenantPublicDto> current() {
        String tenantId = TenantContext.getTenantId();
        return tenantRepository.findById(tenantId)
                .map(this::toPublic)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{code}")
    public ResponseEntity<TenantPublicDto> byCode(@PathVariable String code) {
        return tenantRepository.findByCode(code.toLowerCase())
                .filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
                .map(this::toPublic)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<TenantPublicDto>> listActive() {
        List<TenantPublicDto> list = tenantRepository.findAll().stream()
                .filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
                .map(this::toPublic)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    private TenantPublicDto toPublic(Tenant t) {
        TenantPublicDto dto = new TenantPublicDto();
        dto.setId(t.getId());
        dto.setCode(t.getCode());
        dto.setDisplayName(t.getDisplayName());
        dto.setFrontendUrl(t.getFrontendUrl());
        dto.setLogoUrl(t.getLogoUrl());
        dto.setPrimaryColor(t.getPrimaryColor());
        dto.setEnabledModules(t.getEnabledModules());
        return dto;
    }
}
