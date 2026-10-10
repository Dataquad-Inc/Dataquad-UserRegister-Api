package com.dataquadinc.dto;

import lombok.Data;

@Data
public class TenantPublicDto {
    private String id;
    private String code;
    private String displayName;
    private String frontendUrl;
    private String logoUrl;
    private String primaryColor;
    private String enabledModules;
}
