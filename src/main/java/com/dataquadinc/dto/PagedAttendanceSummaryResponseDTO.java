package com.dataquadinc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

/**
 * Paginated summary view - lighter than the grid view (no per-day attendance map).
 * Returns cycle metadata + summary rows for one page of employees.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedAttendanceSummaryResponseDTO {

    // ── Cycle metadata ──
    private Long    cycleId;
    private String  cycleStatus;
    private String  attendanceMonth;
    private Integer attendanceYear;
    private LocalDate startDate;
    private LocalDate endDate;

    // ── Summary rows for this page ──
    private List<EmployeeSummaryRow> employees;

    // ── KPIs (calculated across ALL employees) ──
    private KpiData kpis;

    // ── Pagination metadata ──
    private int     pageNumber;
    private int     pageSize;
    private long    totalElements;
    private int     totalPages;
    private boolean first;
    private boolean last;

    // ── Constructors ──
    public PagedAttendanceSummaryResponseDTO() {}

    // ── Inner classes ──

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmployeeSummaryRow {
        private String employeeId;
        private String employeeName;
        private String designation;
        private String department;
        private String reportingManager;
        private Boolean hasPF;
        private Boolean hasESI;
        private Boolean isOnProbation;
        private SummaryData summary;

        public EmployeeSummaryRow() {}

        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

        public String getDesignation() { return designation; }
        public void setDesignation(String designation) { this.designation = designation; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getReportingManager() { return reportingManager; }
        public void setReportingManager(String reportingManager) { this.reportingManager = reportingManager; }

        public Boolean getHasPF() { return hasPF; }
        public void setHasPF(Boolean hasPF) { this.hasPF = hasPF; }

        public Boolean getHasESI() { return hasESI; }
        public void setHasESI(Boolean hasESI) { this.hasESI = hasESI; }

        public Boolean getIsOnProbation() { return isOnProbation; }
        public void setIsOnProbation(Boolean isOnProbation) { this.isOnProbation = isOnProbation; }

        public SummaryData getSummary() { return summary; }
        public void setSummary(SummaryData summary) { this.summary = summary; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SummaryData {
        private Integer totalWorkedDays;
        private Integer totalLeavesTaken;
        private Integer totalPayDays;
        private Integer totalWorkingDays;
        private Integer totalWeekOffs;
        private Integer totalPublicHolidays;
        private Integer casualLeaves;
        private Integer sickLeaves;
        private Integer earnedLeaves;
        private Integer lossOfPayLeaves;
        private Double  attendancePercentage;

        public SummaryData() {}

        public Integer getTotalWorkedDays() { return totalWorkedDays; }
        public void setTotalWorkedDays(Integer totalWorkedDays) { this.totalWorkedDays = totalWorkedDays; }

        public Integer getTotalLeavesTaken() { return totalLeavesTaken; }
        public void setTotalLeavesTaken(Integer totalLeavesTaken) { this.totalLeavesTaken = totalLeavesTaken; }

        public Integer getTotalPayDays() { return totalPayDays; }
        public void setTotalPayDays(Integer totalPayDays) { this.totalPayDays = totalPayDays; }

        public Integer getTotalWorkingDays() { return totalWorkingDays; }
        public void setTotalWorkingDays(Integer totalWorkingDays) { this.totalWorkingDays = totalWorkingDays; }

        public Integer getTotalWeekOffs() { return totalWeekOffs; }
        public void setTotalWeekOffs(Integer totalWeekOffs) { this.totalWeekOffs = totalWeekOffs; }

        public Integer getTotalPublicHolidays() { return totalPublicHolidays; }
        public void setTotalPublicHolidays(Integer totalPublicHolidays) { this.totalPublicHolidays = totalPublicHolidays; }

        public Integer getCasualLeaves() { return casualLeaves; }
        public void setCasualLeaves(Integer casualLeaves) { this.casualLeaves = casualLeaves; }

        public Integer getSickLeaves() { return sickLeaves; }
        public void setSickLeaves(Integer sickLeaves) { this.sickLeaves = sickLeaves; }

        public Integer getEarnedLeaves() { return earnedLeaves; }
        public void setEarnedLeaves(Integer earnedLeaves) { this.earnedLeaves = earnedLeaves; }

        public Integer getLossOfPayLeaves() { return lossOfPayLeaves; }
        public void setLossOfPayLeaves(Integer lossOfPayLeaves) { this.lossOfPayLeaves = lossOfPayLeaves; }

        public Double getAttendancePercentage() { return attendancePercentage; }
        public void setAttendancePercentage(Double attendancePercentage) { this.attendancePercentage = attendancePercentage; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class KpiData {
        private Double avgAttendancePct;
        private Integer perfectAttendanceCount;
        private Integer onLeaveCount;
        private Integer below75PctCount;

        public KpiData() {}

        public Double getAvgAttendancePct() { return avgAttendancePct; }
        public void setAvgAttendancePct(Double avgAttendancePct) { this.avgAttendancePct = avgAttendancePct; }

        public Integer getPerfectAttendanceCount() { return perfectAttendanceCount; }
        public void setPerfectAttendanceCount(Integer perfectAttendanceCount) { this.perfectAttendanceCount = perfectAttendanceCount; }

        public Integer getOnLeaveCount() { return onLeaveCount; }
        public void setOnLeaveCount(Integer onLeaveCount) { this.onLeaveCount = onLeaveCount; }

        public Integer getBelow75PctCount() { return below75PctCount; }
        public void setBelow75PctCount(Integer below75PctCount) { this.below75PctCount = below75PctCount; }
    }

    // ── Root level getters and setters ──
    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public String getCycleStatus() { return cycleStatus; }
    public void setCycleStatus(String cycleStatus) { this.cycleStatus = cycleStatus; }

    public String getAttendanceMonth() { return attendanceMonth; }
    public void setAttendanceMonth(String attendanceMonth) { this.attendanceMonth = attendanceMonth; }

    public Integer getAttendanceYear() { return attendanceYear; }
    public void setAttendanceYear(Integer attendanceYear) { this.attendanceYear = attendanceYear; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<EmployeeSummaryRow> getEmployees() { return employees; }
    public void setEmployees(List<EmployeeSummaryRow> employees) { this.employees = employees; }

    public KpiData getKpis() { return kpis; }
    public void setKpis(KpiData kpis) { this.kpis = kpis; }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
}