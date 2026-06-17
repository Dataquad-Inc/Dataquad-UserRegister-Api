package com.dataquadinc.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeAttendanceViewDto {

    private LocalDate attendanceDate;

    private String attendanceStatus;

    private Double attendanceValue;

    private String remarks;

    private Integer weekNumber;

    private Boolean isWeekend;

    private Boolean isPublicHoliday;
}