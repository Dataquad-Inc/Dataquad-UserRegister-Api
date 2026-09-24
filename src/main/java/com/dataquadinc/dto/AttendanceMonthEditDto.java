package com.dataquadinc.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceMonthEditDto {

    private LocalDate date;

    private String attendanceStatus;

    private Double attendanceValue;

    private String remarks;
}