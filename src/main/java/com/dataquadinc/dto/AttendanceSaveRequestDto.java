package com.dataquadinc.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AttendanceSaveRequestDto {

    private LocalDate attendanceDate;

    private Integer attendanceMonth;

    private Integer attendanceYear;

    private String entity;

    private List<EmployeeAttendanceDto> employees;
}