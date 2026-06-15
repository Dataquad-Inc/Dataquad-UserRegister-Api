package com.dataquadinc.dto;

import lombok.Data;

@Data
public class WeekSubmitRequestDto {

    private Integer month;

    private Integer year;

    private Integer weekNumber;
}