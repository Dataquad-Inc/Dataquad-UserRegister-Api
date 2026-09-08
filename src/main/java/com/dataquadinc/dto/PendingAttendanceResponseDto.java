package com.dataquadinc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingAttendanceResponseDto {

    private String employeeId;
    private String employeeName;
    private String designation;
    private Map<String, String> submittedDates;
}
