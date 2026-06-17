package com.dataquadinc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Paginated version of BulkCycleAttendanceResponseDTO.
 * Used by GET /attendance/cycles/{cycleId}/bulk/paged?page=0&size=10&search=
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedBulkCycleAttendanceResponseDTO {

    // ── Cycle metadata ──
    private Long    cycleId;
    private String  cycleStatus;
    private String  attendanceMonth;
    private Integer attendanceYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDaysInCycle;
    private Integer totalWorkingDays;
    private Integer totalWeekOffs;
    private Integer totalPublicHolidays;

    // ── Holiday / week-off info ──
    private List<String>         holidayDates;
    private Map<String, String>  holidayNames;
    private List<Integer>        weekOffDays;

    // ── Employee rows for THIS page ──
    private List<BulkCycleAttendanceResponseDTO.EmployeeAttendanceRow> employees;

    // ── Pagination metadata ──
    private int     pageNumber;
    private int     pageSize;
    private long    totalElements;
    private int     totalPages;
    private boolean first;
    private boolean last;

    // ── Constructors ──
    public PagedBulkCycleAttendanceResponseDTO() {}

    // ── Getters and Setters ──
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

    public Integer getTotalDaysInCycle() { return totalDaysInCycle; }
    public void setTotalDaysInCycle(Integer totalDaysInCycle) { this.totalDaysInCycle = totalDaysInCycle; }

    public Integer getTotalWorkingDays() { return totalWorkingDays; }
    public void setTotalWorkingDays(Integer totalWorkingDays) { this.totalWorkingDays = totalWorkingDays; }

    public Integer getTotalWeekOffs() { return totalWeekOffs; }
    public void setTotalWeekOffs(Integer totalWeekOffs) { this.totalWeekOffs = totalWeekOffs; }

    public Integer getTotalPublicHolidays() { return totalPublicHolidays; }
    public void setTotalPublicHolidays(Integer totalPublicHolidays) { this.totalPublicHolidays = totalPublicHolidays; }

    public List<String> getHolidayDates() { return holidayDates; }
    public void setHolidayDates(List<String> holidayDates) { this.holidayDates = holidayDates; }

    public Map<String, String> getHolidayNames() { return holidayNames; }
    public void setHolidayNames(Map<String, String> holidayNames) { this.holidayNames = holidayNames; }

    public List<Integer> getWeekOffDays() { return weekOffDays; }
    public void setWeekOffDays(List<Integer> weekOffDays) { this.weekOffDays = weekOffDays; }

    public List<BulkCycleAttendanceResponseDTO.EmployeeAttendanceRow> getEmployees() { return employees; }
    public void setEmployees(List<BulkCycleAttendanceResponseDTO.EmployeeAttendanceRow> employees) { this.employees = employees; }

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