package com.dataquadinc.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementLookupDto {
    private String id;
    private String userId;
    private String candidateEmailId;
    private LocalDate endDate;
}