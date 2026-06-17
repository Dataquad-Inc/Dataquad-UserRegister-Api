package com.dataquadinc.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BulkAttendanceResponseDTO {
    private Boolean success;
    private String message;
    private LocalDate attendanceDate;
    private Integer totalEmployees;
    private Integer presentMarked;
    private Integer weekOffMarked;
    private Integer holidayMarked;
    private Long processingTimeMs;
}