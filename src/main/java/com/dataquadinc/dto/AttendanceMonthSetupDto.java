package com.dataquadinc.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AttendanceMonthSetupDto {

    private Integer month;

    private Integer year;

    private List<LocalDate> publicHolidays;

    private String entity;
}