package com.dataquadinc.controller;

import com.dataquadinc.dto.*;
import com.dataquadinc.model.*;
import com.dataquadinc.service.AttendanceManagementService;
import com.dataquadinc.service.HolidayService;
import com.dataquadinc.service.WeekOffConfigService;
import com.dataquadinc.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users/attendance")
@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000", "https://mymulya.com", "http://localhost:3000","http://192.168.0.135:8080","http://192.168.0.135",
        "http://154.210.288.26",
        "http://192.168.0.203:3000",
        "http://192.168.0.167:3000"})
@RequiredArgsConstructor
public class AttendanceManagementController {

    private final AttendanceManagementService attendanceService;
    private final HolidayService holidayService;
    private final WeekOffConfigService weekOffConfigService;
    private final EmployeeService employeeService;

    // Employee APIs
    @GetMapping("/employees")
    public ResponseEntity<List<UserDetails>> getAllActiveEmployees() {
        return ResponseEntity.ok(employeeService.getActiveINEmpolyees());
    }

    @GetMapping("/employees/{userId}")
    public ResponseEntity<UserDetails> getEmployeeById(@PathVariable String userId) {
        return ResponseEntity.ok(employeeService.getEmployeeById(userId));
    }

    // ==================== ATTENDANCE CYCLE APIS ====================

    @GetMapping("/cycles/{cycleId}/bulk")
    public ResponseEntity<BulkCycleAttendanceResponseDTO> getBulkCycleAttendance(
            @PathVariable Long cycleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "false") boolean includeSummary) {
        return ResponseEntity.ok(attendanceService.getBulkCycleAttendance(cycleId, page, size, search, department, includeSummary));
    }

    @PostMapping("/cycles")
    public ResponseEntity<CycleResponseDTO> createCycle(
            @Valid @RequestBody AttendanceCycleDTO cycleDTO,
            @RequestHeader("userId") String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.createCycle(cycleDTO, userId));
    }

    @GetMapping("/cycles")
    public ResponseEntity<List<CycleResponseDTO>> getAllCycles() {
        return ResponseEntity.ok(attendanceService.getAllCycles());
    }

    @GetMapping("/cycles/{cycleId}")
    public ResponseEntity<CycleResponseDTO> getCycleById(@PathVariable Long cycleId) {
        return ResponseEntity.ok(attendanceService.getCycleById(cycleId));
    }

    @PutMapping("/cycles/{cycleId}")
    public ResponseEntity<CycleResponseDTO> updateCycle(
            @PathVariable Long cycleId,
            @Valid @RequestBody AttendanceCycleDTO cycleDTO,
            @RequestHeader("userId") String userId) {
        return ResponseEntity.ok(attendanceService.updateCycle(cycleId, cycleDTO, userId));
    }

    @PutMapping("/cycles/{cycleId}/close")
    public ResponseEntity<Void> closeCycle(
            @PathVariable Long cycleId,
            @RequestHeader("userId") String userId) {
        attendanceService.closeCycle(cycleId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cycles/{cycleId}/generate")
    public ResponseEntity<Void> generateMonthlyAttendance(
            @PathVariable Long cycleId,
            @RequestHeader("userId") String userId) {
        attendanceService.generateMonthlyAttendance(cycleId, userId);
        return ResponseEntity.accepted().build();
    }

    // ==================== HOLIDAY APIS ====================

    @PostMapping("/holidays")
    public ResponseEntity<Holiday> createHoliday(
            @Valid @RequestBody HolidayDTO holidayDTO,
            @RequestHeader("userId") String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(holidayService.createHoliday(holidayDTO, userId));
    }

    @GetMapping("/holidays")
    public ResponseEntity<List<HolidayDTO>> getAllHolidays() {
        return ResponseEntity.ok(holidayService.getAllHolidays());
    }

    @GetMapping("/holidays/{holidayId}")
    public ResponseEntity<HolidayDTO> getHolidayById(@PathVariable Long holidayId) {
        return ResponseEntity.ok(holidayService.getHolidayById(holidayId));
    }

    @PutMapping("/holidays/{holidayId}")
    public ResponseEntity<Holiday> updateHoliday(
            @PathVariable Long holidayId,
            @Valid @RequestBody HolidayDTO holidayDTO,
            @RequestHeader("userId") String userId) {
        return ResponseEntity.ok(holidayService.updateHoliday(holidayId, holidayDTO, userId));
    }

    @DeleteMapping("/holidays/{holidayId}")
    public ResponseEntity<Void> deleteHoliday(
            @PathVariable Long holidayId,
            @RequestHeader("userId") String userId) {
        holidayService.deleteHoliday(holidayId, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== WEEK OFF CONFIGURATION APIS ====================

    @PostMapping("/weekoff/configure")
    public ResponseEntity<WeekOffConfig> configureWeekOff(
            @RequestParam Integer dayOfWeek,
            @RequestParam Boolean isWeekOff,
            @RequestParam(defaultValue = "IN") String entity) {
        return ResponseEntity.ok(weekOffConfigService.configureWeekOff(dayOfWeek, isWeekOff, entity));
    }

    @GetMapping("/weekoff/days")
    public ResponseEntity<List<Integer>> getWeekOffDays(
            @RequestParam(defaultValue = "IN") String entity) {
        return ResponseEntity.ok(weekOffConfigService.getWeekOffDays(entity));
    }

    @GetMapping("/weekoff/check/{dayOfWeek}")
    public ResponseEntity<Boolean> isWeekOff(
            @PathVariable Integer dayOfWeek,
            @RequestParam(defaultValue = "IN") String entity) {
        return ResponseEntity.ok(weekOffConfigService.isWeekOff(dayOfWeek, entity));
    }

    @PostMapping("/weekoff/reset")
    public ResponseEntity<Void> resetWeekOffToDefault(@RequestParam(defaultValue = "IN") String entity) {
        weekOffConfigService.resetToDefault(entity);
        return ResponseEntity.ok().build();
    }

    // ==================== ATTENDANCE OPERATIONS APIS ====================

    @PostMapping("/generate-daily")
    public ResponseEntity<BulkAttendanceResponseDTO> generateDailyAttendance(
            @Valid @RequestBody BulkAttendanceRequestDTO request,
            @RequestHeader("userId") String userId) {
        return ResponseEntity.ok(attendanceService.generateDailyAttendance(request, userId));
    }

    @PostMapping("/mark")
    public ResponseEntity<DailyAttendanceDetail> markAttendance(
            @Valid @RequestBody MarkAttendanceRequestDTO request,
            @RequestHeader("userId") String userId) {
        return ResponseEntity.ok(attendanceService.updateAttendance(request, userId));
    }

    @PutMapping("/update")
    public ResponseEntity<DailyAttendanceDetail> updateAttendance(
            @Valid @RequestBody MarkAttendanceRequestDTO request,
            @RequestHeader("userId") String userId) {
        return ResponseEntity.ok(attendanceService.updateAttendance(request, userId));
    }

    @GetMapping("/employee/{employeeId}/cycle/{cycleId}")
    public ResponseEntity<List<AttendanceGridResponseDTO>> getEmployeeAttendance(
            @PathVariable String employeeId,
            @PathVariable Long cycleId) {
        return ResponseEntity.ok(attendanceService.getAttendanceGrid(employeeId, cycleId));
    }

    @GetMapping("/grid/{employeeId}/cycle/{cycleId}")
    public ResponseEntity<List<AttendanceGridResponseDTO>> getAttendanceGrid(
            @PathVariable String employeeId,
            @PathVariable Long cycleId) {
        return ResponseEntity.ok(attendanceService.getAttendanceGrid(employeeId, cycleId));
    }

    @GetMapping("/summary/{employeeId}/cycle/{cycleId}")
    public ResponseEntity<AttendanceSummaryResponseDTO> getAttendanceSummary(
            @PathVariable String employeeId,
            @PathVariable Long cycleId) {
        return ResponseEntity.ok(attendanceService.getAttendanceSummary(employeeId, cycleId));
    }

    @GetMapping("/monthly")
    public ResponseEntity<PaginatedResponseDTO<AttendanceGridResponseDTO>> getMonthlyAttendance(
            @RequestParam Integer year,
            @RequestParam String month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "attendanceDate") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        MonthlyAttendanceRequestDTO request = new MonthlyAttendanceRequestDTO();
        request.setYear(year);
        request.setMonth(month);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        return ResponseEntity.ok(attendanceService.getMonthlyAttendance(request));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<AttendanceGridResponseDTO>> getAttendanceByDateRange(
            @RequestParam String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(attendanceService.getAttendanceByDateRange(employeeId, startDate, endDate));
    }

    @GetMapping("/stats/cycle/{cycleId}")
    public ResponseEntity<Map<String, Object>> getAttendanceStatistics(@PathVariable Long cycleId) {
        return ResponseEntity.ok(attendanceService.getAttendanceStatistics(cycleId));
    }

    @PostMapping("/bulk-update")
    public ResponseEntity<Void> bulkUpdateAttendance(
            @RequestBody List<MarkAttendanceRequestDTO> requests,
            @RequestHeader("userId") String userId) {
        attendanceService.processBulkAttendanceUpdate(requests, userId);
        return ResponseEntity.accepted().build();
    }
}