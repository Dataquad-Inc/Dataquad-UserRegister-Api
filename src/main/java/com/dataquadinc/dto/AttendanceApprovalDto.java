package com.dataquadinc.dto;

import lombok.Data;

@Data
public class AttendanceApprovalDto {

    private Integer month;

    private Integer year;

    private Integer weekNumber;

    private String approvedBy;
}