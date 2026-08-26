package com.dataquadinc.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class AttendanceDashboardResponseDto {

    private Integer serialNo;

    private String employeeId;

    private String employeeName;

    private String reportingManager;

    private String designation;

    private LocalDate joiningDate;

    /*
    YES / NO
     */
    private String pf;

    /*
    YES / NO
     */
    private String esi;

    /*
    Completed / Not Completed
     */
    private String probation;

    /*
    day → attendance

    Example:
    26 -> P
    27 -> WO
    28 -> L
     */
    private Map<String, String> attendanceGrid;

    private Integer totalDaysInMonth;

    private Integer totalWorkingDays;

    private Integer totalWeekendDays;

    private Double totalPresentDays;

    private Integer totalLeaves;

    private Integer casualLeaves;

    private Double totalPaidDays;

    private int totalLop;

    private int totalHalfDays;

    private int totalWfH;

    private int totalPublicHolidays;

    private int totalWeekOffs;
}