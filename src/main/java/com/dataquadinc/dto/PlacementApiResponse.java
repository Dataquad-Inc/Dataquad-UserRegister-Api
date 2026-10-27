package com.dataquadinc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementApiResponse {

    private boolean success;
    private String message;
    private String timestamp;
    private List<PlacementLookupDto> data;
}
