package com.dataquadinc.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ApprovedAttendanceSummaryDto {

    private Integer serialNo;

    private String employeeId;

    private String employeeName;

    private String reportingManager;

    private String designation;

    private LocalDate joiningDate;

    private String pf;

    private String esi;

    private String probation;

    private Integer totalDaysInMonth;

    private Integer totalWorkingDays;

    private Integer totalWeekendDays;

    private Double totalPresentDays;

    private Integer totalLeaves;

    private Integer casualLeaves;

    private Double totalPaidDays;

    private Integer totalLop;

    private Integer totalHalfDays;

    private Integer totalWfH;

    private Integer totalPublicHolidays;

    private Integer totalWeekOffs;
}
