package com.dataquadinc.dto;

import lombok.Data;

@Data
public class EmployeeAttendanceDto {

    private String employeeId;

    /*
    P,L,WH,HD,LL
     */
    private String attendanceStatus;

    /*
    for HD editable
    0.5 or 1
     */
    private Double attendanceValue;

    private String remarks;
}
