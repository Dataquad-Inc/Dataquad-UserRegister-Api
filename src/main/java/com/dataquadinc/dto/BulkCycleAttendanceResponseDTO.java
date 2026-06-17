package com.dataquadinc.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class BulkCycleAttendanceResponseDTO {

    private Long cycleId;
    private String cycleStatus;
    private String attendanceMonth;
    private Integer attendanceYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDaysInCycle;
    private Integer totalWorkingDays;
    private Integer totalWeekOffs;
    private Integer totalPublicHolidays;
    private List<String> holidayDates;
    private Map<String, String> holidayNames;
    private List<Integer> weekOffDays;
    private Integer totalEmployees;
    private List<EmployeeAttendanceRow> employees;

    @Data
    public static class EmployeeAttendanceRow {
        private String employeeId;
        private String employeeName;
        private String designation;
        private String department;
        private String reportingManager;
        private Boolean hasPF;
        private Boolean hasESI;
        private Boolean isOnProbation;
        private Map<String, String> attendance;
        private SummaryRow summary;
    }

    @Data
    public static class SummaryRow {
        private Integer totalWorkedDays;
        private Integer totalLeavesTaken;
        private Integer totalPayDays;
        private Integer totalWorkingDays;
        private Integer totalWeekOffs;
        private Integer totalPublicHolidays;
        private Integer casualLeaves;
        private Integer sickLeaves;
        private Integer lossOfPayLeaves;
        private Integer specialLeaves;
        private Double attendancePercentage;
    }
}