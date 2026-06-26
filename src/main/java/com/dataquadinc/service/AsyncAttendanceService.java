package com.dataquadinc.service;

import com.dataquadinc.model.AttendanceCycle;
import com.dataquadinc.model.DailyAttendanceDetail;
import com.dataquadinc.model.DailyAttendanceDetail.AttendanceStatus;
import com.dataquadinc.model.EmployeeAttendanceSummary;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.WeekOffConfig;
import com.dataquadinc.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AsyncAttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AsyncAttendanceService.class);

    private final DailyAttendanceRepository attendanceRepository;
    private final AttendanceCycleRepository cycleRepository;
    private final HolidayRepository holidayRepository;
    private final WeekOffConfigRepository weekOffConfigRepository;
    private final EmployeeAttendanceSummaryRepository summaryRepository;
    private final UserDao userDao;

    public AsyncAttendanceService(
            DailyAttendanceRepository attendanceRepository,
            AttendanceCycleRepository cycleRepository,
            HolidayRepository holidayRepository,
            WeekOffConfigRepository weekOffConfigRepository,
            EmployeeAttendanceSummaryRepository summaryRepository,
            UserDao userDao) {
        this.attendanceRepository = attendanceRepository;
        this.cycleRepository = cycleRepository;
        this.holidayRepository = holidayRepository;
        this.weekOffConfigRepository = weekOffConfigRepository;
        this.summaryRepository = summaryRepository;
        this.userDao = userDao;
    }

    /**
     * Asynchronously generates attendance records for all active employees
     * for the entire cycle. Returns immediately, processing happens in background.
     */
    @Async("attendanceTaskExecutor")
    @Transactional
    public void generateMonthlyAttendanceAsync(Long cycleId, String generatedBy) {
        long startTime = System.currentTimeMillis();
        log.info("=== Async attendance generation STARTED for cycle {} ===", cycleId);

        try {
            // Small delay to ensure transaction is fully committed and visible
            Thread.sleep(1000);

            AttendanceCycle cycle = cycleRepository.findById(cycleId).orElse(null);
            if (cycle == null) {
                log.error("Cycle not found: {}. This should not happen if transaction committed properly.", cycleId);
                return;
            }

            if ("CLOSED".equals(cycle.getStatus())) {
                log.warn("Cannot generate attendance for closed cycle {}", cycleId);
                return;
            }

            List<UserDetails> employees = userDao.findAllActiveInEmployeesExcludingExternal();
            if (employees.isEmpty()) {
                log.warn("No active employees found for cycle {}", cycleId);
                return;
            }

            log.info("Generating attendance for {} employees in cycle {} ({} to {})",
                    employees.size(), cycleId, cycle.getStartDate(), cycle.getEndDate());

            // Pre-fetch holidays and week-offs for the entire cycle
            Set<LocalDate> holidays = new HashSet<>(
                    holidayRepository.findHolidayDatesBetween(cycle.getStartDate(), cycle.getEndDate())
            );

            Set<Integer> weekOffDays = weekOffConfigRepository.findByEntity("IN").stream()
                    .filter(c -> Boolean.TRUE.equals(c.getIsWeekOff()))
                    .map(WeekOffConfig::getDayOfWeek)
                    .collect(Collectors.toSet());

            // Build date-status map once
            Map<LocalDate, AttendanceStatus> dateStatusMap = new LinkedHashMap<>();
            int workingDays = 0, weekOffs = 0, publicHolidays = 0;

            for (LocalDate d = cycle.getStartDate(); !d.isAfter(cycle.getEndDate()); d = d.plusDays(1)) {
                AttendanceStatus status;
                if (holidays.contains(d)) {
                    status = AttendanceStatus.PH;
                    publicHolidays++;
                } else if (weekOffDays.contains(d.getDayOfWeek().getValue())) {
                    status = AttendanceStatus.WO;
                    weekOffs++;
                } else {
                    status = AttendanceStatus.P;
                    workingDays++;
                }
                dateStatusMap.put(d, status);
            }

            log.info("Cycle {} date map: {} working, {} week-offs, {} holidays, {} total days",
                    cycleId, workingDays, weekOffs, publicHolidays, dateStatusMap.size());

            // Fetch existing records in one query
            Set<String> existingRecords = attendanceRepository.findAllByCycleId(cycleId)
                    .stream()
                    .map(a -> a.getEmployeeId() + "_" + a.getAttendanceDate())
                    .collect(Collectors.toSet());

            log.info("Existing records found: {}", existingRecords.size());

            // Generate records in batches for better performance
            List<DailyAttendanceDetail> batch = new ArrayList<>();
            int batchSize = 1000; // Adjust based on your DB performance
            int totalInserted = 0;
            int skippedCount = 0;

            for (UserDetails emp : employees) {
                for (Map.Entry<LocalDate, AttendanceStatus> entry : dateStatusMap.entrySet()) {
                    String key = emp.getUserId() + "_" + entry.getKey();
                    if (!existingRecords.contains(key)) {
                        DailyAttendanceDetail detail = new DailyAttendanceDetail();
                        detail.setEmployeeId(emp.getUserId());
                        detail.setAttendanceCycle(cycle);
                        detail.setAttendanceDate(entry.getKey());
                        detail.setStatus(entry.getValue());
                        detail.setMarkedBy(generatedBy);
                        batch.add(detail);

                        if (batch.size() >= batchSize) {
                            attendanceRepository.saveAll(batch);
                            totalInserted += batch.size();
                            log.info("Progress: inserted {} records so far for cycle {}", totalInserted, cycleId);
                            batch.clear();
                        }
                    } else {
                        skippedCount++;
                    }
                }
            }

            // Insert remaining records
            if (!batch.isEmpty()) {
                attendanceRepository.saveAll(batch);
                totalInserted += batch.size();
            }

            log.info("Inserted {} new attendance records, skipped {} existing records for cycle {}",
                    totalInserted, skippedCount, cycleId);

            // Update summaries after all records are generated
            if (totalInserted > 0 || skippedCount > 0) {
                updateAttendanceSummaryDirectly(cycleId, cycle, employees);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("=== Async attendance generation COMPLETED for cycle {} in {}ms. {} records inserted, {} skipped ===",
                    cycleId, duration, totalInserted, skippedCount);

        } catch (Exception e) {
            log.error("Failed to generate attendance for cycle {}: {}", cycleId, e.getMessage(), e);
        }
    }

    /**
     * Direct implementation of updateAttendanceSummary to avoid circular dependency.
     */
    @Transactional
    public void updateAttendanceSummaryDirectly(Long cycleId, AttendanceCycle cycle, List<UserDetails> employees) {
        try {
            // Load ALL attendance for this cycle in one query, group in memory
            Map<String, List<DailyAttendanceDetail>> byEmployee =
                    attendanceRepository.findAllByCycleId(cycleId)
                            .stream()
                            .collect(Collectors.groupingBy(DailyAttendanceDetail::getEmployeeId));

            // Load all existing summaries
            Map<String, EmployeeAttendanceSummary> existingSummaries =
                    summaryRepository.findByAttendanceCycle_CycleId(cycleId)
                            .stream()
                            .collect(Collectors.toMap(EmployeeAttendanceSummary::getEmployeeId, s -> s));

            List<EmployeeAttendanceSummary> toSave = new ArrayList<>();

            for (UserDetails emp : employees) {
                List<DailyAttendanceDetail> records = byEmployee.getOrDefault(emp.getUserId(), List.of());
                if (records.isEmpty()) continue;

                EmployeeAttendanceSummary summary = existingSummaries.getOrDefault(
                        emp.getUserId(), new EmployeeAttendanceSummary());
                summary.setEmployeeId(emp.getUserId());
                summary.setAttendanceCycle(cycle);

                Map<AttendanceStatus, Long> cnt = records.stream()
                        .collect(Collectors.groupingBy(DailyAttendanceDetail::getStatus, Collectors.counting()));


                int actualWeekOffs =
                        cnt.getOrDefault(AttendanceStatus.WO, 0L).intValue();

                int actualPublicHolidays =
                        cnt.getOrDefault(AttendanceStatus.PH, 0L).intValue();

                summary.setTotalWeekOffs(actualWeekOffs);
                summary.setTotalPublicHolidays(actualPublicHolidays);

                summary.setTotalWorkingDays(
                        cycle.getTotalDaysInCycle()
                                - actualWeekOffs
                                - actualPublicHolidays
                );
                summary.setCasualLeaves(cnt.getOrDefault(AttendanceStatus.CL,  0L).intValue());
                summary.setSickLeaves(cnt.getOrDefault(AttendanceStatus.SL,    0L).intValue());
                summary.setLossOfPayLeaves(cnt.getOrDefault(AttendanceStatus.LOP, 0L).intValue());
                summary.setSpecialLeaves(cnt.getOrDefault(AttendanceStatus.SP,  0L).intValue());

                summary.setTotalLeavesTaken(
                        summary.getCasualLeaves() + summary.getSickLeaves()
                                + summary.getLossOfPayLeaves() + summary.getSpecialLeaves());

                double worked = cnt.getOrDefault(AttendanceStatus.P, 0L)
                        + cnt.getOrDefault(AttendanceStatus.WFH, 0L)
                        + cnt.getOrDefault(AttendanceStatus.HD, 0L) * 0.5;
                summary.setTotalWorkedDays((int) Math.floor(worked));

                double payDays = worked
                        + cnt.getOrDefault(AttendanceStatus.WO,  0L)
                        + cnt.getOrDefault(AttendanceStatus.PH,  0L)
                        + cnt.getOrDefault(AttendanceStatus.CL,  0L)
                        + cnt.getOrDefault(AttendanceStatus.SL,  0L)
                        + cnt.getOrDefault(AttendanceStatus.SP,  0L);
                summary.setTotalPayDays((int) Math.floor(payDays));

                toSave.add(summary);
            }

            if (!toSave.isEmpty()) {
                summaryRepository.saveAll(toSave);
                log.info("Updated summaries for {} employees in cycle {}", toSave.size(), cycleId);
            }
        } catch (Exception e) {
            log.error("Failed to update attendance summary for cycle {}: {}", cycleId, e.getMessage(), e);
        }
    }
}