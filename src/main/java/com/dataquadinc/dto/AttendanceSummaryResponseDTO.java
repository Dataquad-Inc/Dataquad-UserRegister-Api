package com.dataquadinc.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AttendanceSummaryResponseDTO {
    private String employeeId;
    private String employeeName;
    private String designation;
    private String department;
    private String attendanceMonth;
    private Integer attendanceYear;
    private Integer totalWorkingDays;
    private Integer totalWeekOffs;
    private Integer totalPublicHolidays;
    private Integer casualLeaves;
    private Integer sickLeaves;
    private Integer lossOfPayLeaves;
    private Integer specialLeaves;
    private Integer totalLeavesTaken;
    private Integer totalWorkedDays;
    private Integer totalPayDays;
    private Double attendancePercentage;
    private Map<String, Integer> leaveBreakdown;
}