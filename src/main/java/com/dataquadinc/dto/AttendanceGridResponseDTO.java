package com.dataquadinc.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AttendanceGridResponseDTO {
    private LocalDate date;
    private String day;
    private String status;
    private String statusName;
    private String remarks;
    private String employeeName;
    private String designation;
}
