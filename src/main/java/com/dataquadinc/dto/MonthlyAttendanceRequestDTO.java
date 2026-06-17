package com.dataquadinc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MonthlyAttendanceRequestDTO {
    @NotNull(message = "Year is required")
    private Integer year;

    @NotBlank(message = "Month is required")
    private String month;

    private String entity = "IN";
    private String department;
    private String status;
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "attendanceDate";
    private String sortDirection = "ASC";
}
