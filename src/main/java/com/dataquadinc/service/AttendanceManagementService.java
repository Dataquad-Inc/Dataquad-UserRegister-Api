package com.dataquadinc.service;

import com.dataquadinc.dto.*;
import com.dataquadinc.exceptions.*;
import com.dataquadinc.model.*;
import com.dataquadinc.model.DailyAttendanceDetail.AttendanceStatus;
import com.dataquadinc.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceManagementService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceManagementService.class);

    private final DailyAttendanceRepository              attendanceRepository;
    private final AttendanceCycleRepository              cycleRepository;
    private final HolidayRepository                     holidayRepository;
    private final EmployeeAttendanceSummaryRepository   summaryRepository;
    private final WeekOffConfigRepository               weekOffConfigRepository;
    private final UserDao                               userDao;
    private final LeavePolicyService                    leavePolicyService;
    private final ApplicationContext                    applicationContext;

    public AttendanceManagementService(
            DailyAttendanceRepository attendanceRepository,
            AttendanceCycleRepository cycleRepository,
            HolidayRepository holidayRepository,
            EmployeeAttendanceSummaryRepository summaryRepository,
            WeekOffConfigRepository weekOffConfigRepository,
            UserDao userDao,
            LeavePolicyService leavePolicyService,
            ApplicationContext applicationContext) {
        this.attendanceRepository = attendanceRepository;
        this.cycleRepository      = cycleRepository;
        this.holidayRepository    = holidayRepository;
        this.summaryRepository    = summaryRepository;
        this.weekOffConfigRepository = weekOffConfigRepository;
        this.userDao              = userDao;
        this.leavePolicyService   = leavePolicyService;
        this.applicationContext   = applicationContext;
    }

    private AsyncAttendanceService getAsyncAttendanceService() {
        return applicationContext.getBean(AsyncAttendanceService.class);
    }

    // ==================== CYCLE MANAGEMENT ====================

    @Transactional
    public CycleResponseDTO createCycle(AttendanceCycleDTO cycleDTO, String createdBy) {
        log.info("Creating attendance cycle for {}/{}", cycleDTO.getAttendanceMonth(), cycleDTO.getAttendanceYear());

        if (cycleDTO.getStartDate().isAfter(cycleDTO.getEndDate())) {
            throw new AttendanceException("Start date must be before end date");
        }

        Optional<AttendanceCycle> existingCycle = cycleRepository.findByAttendanceYearAndAttendanceMonth(
                cycleDTO.getAttendanceYear(), cycleDTO.getAttendanceMonth().toUpperCase());
        if (existingCycle.isPresent()) {
            throw new CycleAlreadyExistsException(
                    "Attendance cycle already exists for " + cycleDTO.getAttendanceMonth()
                            + " " + cycleDTO.getAttendanceYear());
        }

        boolean overlaps = cycleRepository.existsOpenCycleOverlapping(
                cycleDTO.getStartDate(), cycleDTO.getEndDate());
        if (overlaps) {
            throw new AttendanceException("Cycle dates overlap with an existing OPEN cycle.");
        }

        AttendanceCycle cycle = new AttendanceCycle();
        cycle.setAttendanceMonth(cycleDTO.getAttendanceMonth().toUpperCase());
        cycle.setAttendanceYear(cycleDTO.getAttendanceYear());
        cycle.setStartDate(cycleDTO.getStartDate());
        cycle.setEndDate(cycleDTO.getEndDate());
        cycle.setTotalDaysInCycle(
                (int) (cycleDTO.getEndDate().toEpochDay() - cycleDTO.getStartDate().toEpochDay() + 1));
        cycle.setStatus("OPEN");
        cycle.setCreatedBy(createdBy);
        calculateCycleMetrics(cycle);

        AttendanceCycle savedCycle = cycleRepository.save(cycle);
        cycleRepository.flush();

        final Long savedCycleId = savedCycle.getCycleId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Transaction COMMITTED for cycle {}. Starting async attendance generation.", savedCycleId);
                getAsyncAttendanceService().generateMonthlyAttendanceAsync(savedCycleId, createdBy);
            }
        });

        return convertToCycleResponseDTO(savedCycle);
    }

    public List<CycleResponseDTO> getAllCycles() {
        return cycleRepository.findAllByOrderByAttendanceYearDescAttendanceMonthDesc()
                .stream()
                .map(this::convertToCycleResponseDTO)
                .collect(Collectors.toList());
    }

    // ==================== OPTIMIZED BULK ATTENDANCE (SERVER-SIDE PAGINATION) ====================

    /**
     * Returns one page of employees with their attendance data for the cycle.
     *
     * <p>Search term is matched (case-insensitive) against:
     * <ul>
     *   <li>employeeName (u.userName)</li>
     *   <li>employeeId (u.userId)</li>
     *   <li>reportingManager (u.reportingManager)</li>
     * </ul>
     *
     * @param cycleId        target cycle
     * @param page           0-based page index
     * @param size           page size
     * @param search         optional free-text search (null → no filter)
     * @param department     optional exact department filter (null → no filter)
     * @param includeSummary ignored – summaries are always fetched
     */
    public BulkCycleAttendanceResponseDTO getBulkCycleAttendance(
            Long cycleId, int page, int size, String search, String department, boolean includeSummary) {

        long startTime = System.currentTimeMillis();

        // Normalise: treat blank search strings the same as null
        String effectiveSearch = (search != null && search.isBlank()) ? null : search;
        String effectiveDept   = (department != null && department.isBlank()) ? null : department;

        // 1. Validate cycle
        AttendanceCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new CycleNotFoundException("Cycle not found: " + cycleId));

        // 2. Paginated employees
        Pageable pageable = PageRequest.of(page, size, Sort.by("userName").ascending());
        Page<UserDetails> employeePage = userDao.findActiveInEmployeesWithFilters(
                effectiveSearch, effectiveDept, pageable);

        List<String> employeeIds = employeePage.getContent().stream()
                .map(UserDetails::getUserId)
                .collect(Collectors.toList());

        if (employeeIds.isEmpty()) {
            return buildEmptyResponse(cycle, page, size, (int) employeePage.getTotalElements(), employeePage.getTotalPages());
        }

        // 3. Attendance data – single native projection query for the page
        List<Object[]> attendanceData = attendanceRepository
                .findAttendanceProjectionByEmployeeIdsNative(cycleId, employeeIds);

        // 4. Build attendance map  empId → (date → status)
        Map<String, Map<String, String>> attendanceByEmployee = new HashMap<>(employeeIds.size());
        for (Object[] row : attendanceData) {
            String empId   = (String) row[0];
            String dateStr = row[1].toString();
            String status  = (String) row[2];
            attendanceByEmployee.computeIfAbsent(empId, k -> new HashMap<>()).put(dateStr, status);
        }

        // 5. Summaries for the current page only
        List<EmployeeAttendanceSummary> summaries = summaryRepository
                .findByAttendanceCycle_CycleIdAndEmployeeIdIn(cycleId, employeeIds);
        Map<String, EmployeeAttendanceSummary> summaryByEmployee = summaries.stream()
                .collect(Collectors.toMap(EmployeeAttendanceSummary::getEmployeeId, s -> s));

        // 6. Holidays and week-offs
        List<Holiday> holidays = holidayRepository.findByHolidayDateBetween(
                cycle.getStartDate(), cycle.getEndDate());
        List<String> holidayDates = holidays.stream()
                .map(h -> h.getHolidayDate().toString())
                .collect(Collectors.toList());
        Map<String, String> holidayNames = holidays.stream()
                .collect(Collectors.toMap(h -> h.getHolidayDate().toString(), Holiday::getHolidayName));

        List<Integer> weekOffDays = weekOffConfigRepository.findByEntity("IN").stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsWeekOff()))
                .map(WeekOffConfig::getDayOfWeek)
                .collect(Collectors.toList());

        // 7. Build employee rows
        List<BulkCycleAttendanceResponseDTO.EmployeeAttendanceRow> rows = employeePage.getContent().stream()
                .map(emp -> buildEmployeeAttendanceRow(emp, attendanceByEmployee, summaryByEmployee, cycle))
                .collect(Collectors.toList());

        // 8. Assemble response with pagination metadata
        BulkCycleAttendanceResponseDTO response = new BulkCycleAttendanceResponseDTO();
        response.setCycleId(cycle.getCycleId());
        response.setCycleStatus(cycle.getStatus());
        response.setAttendanceMonth(cycle.getAttendanceMonth());
        response.setAttendanceYear(cycle.getAttendanceYear());
        response.setStartDate(cycle.getStartDate());
        response.setEndDate(cycle.getEndDate());
        response.setTotalDaysInCycle(cycle.getTotalDaysInCycle());
        response.setTotalWorkingDays(cycle.getTotalWorkingDays());
        response.setTotalWeekOffs(cycle.getTotalWeekOffs());
        response.setTotalPublicHolidays(cycle.getTotalPublicHolidays());
        response.setHolidayDates(holidayDates);
        response.setHolidayNames(holidayNames);
        response.setWeekOffDays(weekOffDays);
        response.setEmployees(rows);

        // Pagination metadata
        response.setTotalEmployees((int) employeePage.getTotalElements());
        response.setTotalPages(employeePage.getTotalPages());
        response.setPageNumber(employeePage.getNumber());
        response.setPageSize(employeePage.getSize());

        log.info("Bulk attendance loaded in {}ms — page {}/{}, {} employees on page, {} total, {} summaries",
                System.currentTimeMillis() - startTime,
                page, employeePage.getTotalPages(),
                rows.size(), employeePage.getTotalElements(),
                summaryByEmployee.size());

        return response;
    }

    private BulkCycleAttendanceResponseDTO buildEmptyResponse(
            AttendanceCycle cycle, int page, int size, int totalElements, int totalPages) {

        BulkCycleAttendanceResponseDTO response = new BulkCycleAttendanceResponseDTO();
        response.setCycleId(cycle.getCycleId());
        response.setCycleStatus(cycle.getStatus());
        response.setAttendanceMonth(cycle.getAttendanceMonth());
        response.setAttendanceYear(cycle.getAttendanceYear());
        response.setStartDate(cycle.getStartDate());
        response.setEndDate(cycle.getEndDate());
        response.setTotalDaysInCycle(cycle.getTotalDaysInCycle());
        response.setTotalWorkingDays(cycle.getTotalWorkingDays());
        response.setTotalWeekOffs(cycle.getTotalWeekOffs());
        response.setTotalPublicHolidays(cycle.getTotalPublicHolidays());
        response.setTotalEmployees(totalElements);
        response.setTotalPages(totalPages);
        response.setPageNumber(page);
        response.setPageSize(size);
        response.setEmployees(new ArrayList<>());

        List<Holiday> holidays = holidayRepository.findByHolidayDateBetween(
                cycle.getStartDate(), cycle.getEndDate());
        response.setHolidayDates(holidays.stream()
                .map(h -> h.getHolidayDate().toString()).collect(Collectors.toList()));
        response.setHolidayNames(holidays.stream()
                .collect(Collectors.toMap(h -> h.getHolidayDate().toString(), Holiday::getHolidayName)));
        response.setWeekOffDays(weekOffConfigRepository.findByEntity("IN").stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsWeekOff()))
                .map(WeekOffConfig::getDayOfWeek).collect(Collectors.toList()));
        return response;
    }

    private BulkCycleAttendanceResponseDTO.EmployeeAttendanceRow buildEmployeeAttendanceRow(
            UserDetails emp,
            Map<String, Map<String, String>> attendanceByEmployee,
            Map<String, EmployeeAttendanceSummary> summaryByEmployee,
            AttendanceCycle cycle) {

        BulkCycleAttendanceResponseDTO.EmployeeAttendanceRow row =
                new BulkCycleAttendanceResponseDTO.EmployeeAttendanceRow();
        row.setEmployeeId(emp.getUserId());
        row.setEmployeeName(emp.getUserName());
        row.setDesignation(emp.getDesignation());
        row.setDepartment(emp.getDepartment());
        row.setReportingManager(emp.getReportingManager());
        row.setHasPF(Boolean.TRUE.equals(emp.getIsEmployeeHavingPF()));
        row.setHasESI(Boolean.TRUE.equals(emp.getIsEmployeeHavingESI()));
        row.setIsOnProbation("YES".equalsIgnoreCase(emp.getProbation()));
        row.setAttendance(attendanceByEmployee.getOrDefault(emp.getUserId(), new HashMap<>()));

        EmployeeAttendanceSummary s = summaryByEmployee.get(emp.getUserId());
        BulkCycleAttendanceResponseDTO.SummaryRow summary = new BulkCycleAttendanceResponseDTO.SummaryRow();

        if (s != null) {
            summary.setTotalWorkedDays(s.getTotalWorkedDays());
            summary.setTotalLeavesTaken(s.getTotalLeavesTaken());
            summary.setTotalPayDays(s.getTotalPayDays());
            summary.setTotalWorkingDays(s.getTotalWorkingDays());
            summary.setTotalWeekOffs(s.getTotalWeekOffs());
            summary.setTotalPublicHolidays(s.getTotalPublicHolidays());
            summary.setCasualLeaves(s.getCasualLeaves());
            summary.setSickLeaves(s.getSickLeaves());
            summary.setLossOfPayLeaves(s.getLossOfPayLeaves());
            summary.setSpecialLeaves(s.getSpecialLeaves());
            if (s.getTotalWorkingDays() != null && s.getTotalWorkingDays() > 0) {
                double pct = (s.getTotalWorkedDays() * 100.0) / s.getTotalWorkingDays();
                summary.setAttendancePercentage(Math.round(pct * 100.0) / 100.0);
            } else {
                summary.setAttendancePercentage(0.0);
            }
        } else {
            summary.setTotalWorkedDays(0);
            summary.setTotalLeavesTaken(0);
            summary.setTotalPayDays(0);
            summary.setTotalWorkingDays(cycle.getTotalWorkingDays());
            summary.setTotalWeekOffs(cycle.getTotalWeekOffs());
            summary.setTotalPublicHolidays(cycle.getTotalPublicHolidays());
            summary.setCasualLeaves(0);
            summary.setSickLeaves(0);
            summary.setLossOfPayLeaves(0);
            summary.setSpecialLeaves(0);
            summary.setAttendancePercentage(0.0);
        }

        row.setSummary(summary);
        return row;
    }

    // ==================== CYCLE METRICS ====================

    private void calculateCycleMetrics(AttendanceCycle cycle) {
        List<LocalDate> holidays =
                holidayRepository.findHolidayDatesBetween(cycle.getStartDate(), cycle.getEndDate());
        Set<LocalDate>  holidaySet  = new HashSet<>(holidays);
        Set<Integer>    weekOffDays = getWeekOffDaysSet();

        int workingDays = 0, weekOffs = 0, publicHolidays = 0;
        LocalDate cur = cycle.getStartDate();

        while (!cur.isAfter(cycle.getEndDate())) {
            int dow = cur.getDayOfWeek().getValue();
            if (weekOffDays.contains(dow))   { weekOffs++; }
            else if (holidaySet.contains(cur)) { publicHolidays++; }
            else                               { workingDays++; }
            cur = cur.plusDays(1);
        }

        cycle.setTotalWorkingDays(workingDays);
        cycle.setTotalWeekOffs(weekOffs);
        cycle.setTotalPublicHolidays(publicHolidays);
        log.debug("Cycle metrics – working: {}, weekOffs: {}, holidays: {}",
                workingDays, weekOffs, publicHolidays);
    }

    private Set<Integer> getWeekOffDaysSet() {
        return weekOffConfigRepository.findByEntity("IN").stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsWeekOff()))
                .map(WeekOffConfig::getDayOfWeek)
                .collect(Collectors.toSet());
    }

    // ==================== CYCLE QUERIES ====================

    public CycleResponseDTO getCycleById(Long cycleId) {
        AttendanceCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new CycleNotFoundException("Cycle not found: " + cycleId));
        return convertToCycleResponseDTO(cycle);
    }

    @Transactional
    public CycleResponseDTO updateCycle(Long cycleId, AttendanceCycleDTO cycleDTO, String updatedBy) {
        AttendanceCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new CycleNotFoundException("Cycle not found: " + cycleId));

        if ("CLOSED".equals(cycle.getStatus())) {
            throw new AttendanceException("Cannot update a closed cycle");
        }
        if (cycleDTO.getStartDate().isAfter(cycleDTO.getEndDate())) {
            throw new AttendanceException("Start date must be before end date");
        }

        cycle.setStartDate(cycleDTO.getStartDate());
        cycle.setEndDate(cycleDTO.getEndDate());
        cycle.setTotalDaysInCycle(
                (int) (cycleDTO.getEndDate().toEpochDay() - cycleDTO.getStartDate().toEpochDay() + 1));
        cycle.setUpdatedBy(updatedBy);
        calculateCycleMetrics(cycle);

        AttendanceCycle updated = cycleRepository.save(cycle);
        cycleRepository.flush();

        final Long updatedCycleId = updated.getCycleId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Transaction COMMITTED for updated cycle {}. Starting async regeneration.", updatedCycleId);
                getAsyncAttendanceService().generateMonthlyAttendanceAsync(updatedCycleId, updatedBy);
            }
        });

        return convertToCycleResponseDTO(updated);
    }

    @Transactional
    public void closeCycle(Long cycleId, String closedBy) {
        AttendanceCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new CycleNotFoundException("Cycle not found: " + cycleId));
        if ("CLOSED".equals(cycle.getStatus())) {
            throw new AttendanceException("Cycle is already closed");
        }
        cycle.setStatus("CLOSED");
        cycle.setUpdatedBy(closedBy);
        cycleRepository.save(cycle);
        log.info("Closed attendance cycle {} ({}/{})", cycleId,
                cycle.getAttendanceMonth(), cycle.getAttendanceYear());
    }

    public CycleResponseDTO getCycleByDate(LocalDate date) {
        AttendanceCycle cycle = cycleRepository.findByDate(date)
                .orElseThrow(() -> new CycleNotFoundException("No cycle found for date: " + date));
        return convertToCycleResponseDTO(cycle);
    }

    public CycleResponseDTO getLatestOpenCycle() {
        AttendanceCycle cycle = cycleRepository.findLatestOpenCycle()
                .orElseThrow(() -> new CycleNotFoundException("No open cycle found"));
        return convertToCycleResponseDTO(cycle);
    }

    // ==================== CYCLE GENERATION STATUS ====================

    public Map<String, Object> getCycleGenerationStatus(Long cycleId) {
        Map<String, Object> status = new LinkedHashMap<>();
        AttendanceCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new CycleNotFoundException("Cycle not found: " + cycleId));

        long totalRecords   = attendanceRepository.countByCycleId(cycleId);
        long expectedRecords = (long) getActiveINEmployees().size() * cycle.getTotalDaysInCycle();

        status.put("cycleId", cycleId);
        status.put("cycleStatus", cycle.getStatus());
        status.put("totalRecordsGenerated", totalRecords);
        status.put("expectedRecords", expectedRecords);
        status.put("generationComplete", totalRecords >= expectedRecords);
        status.put("progressPercentage", expectedRecords > 0
                ? Math.round((totalRecords * 100.0) / expectedRecords * 100.0) / 100.0 : 0.0);
        status.put("attendanceMonth", cycle.getAttendanceMonth());
        status.put("attendanceYear", cycle.getAttendanceYear());
        return status;
    }

    // ==================== DAILY ATTENDANCE GENERATION ====================

    @Transactional
    public BulkAttendanceResponseDTO generateDailyAttendance(BulkAttendanceRequestDTO request, String markedBy) {
        long startTime = System.currentTimeMillis();
        LocalDate attendanceDate = request.getAttendanceDate();

        if (attendanceDate.isAfter(LocalDate.now())) {
            throw new FutureDateAttendanceException(
                    "Cannot mark attendance for a future date: " + attendanceDate);
        }

        AttendanceCycle cycle = getOrCreateCycleForDate(attendanceDate);
        List<UserDetails> activeEmployees = getActiveINEmployees();
        if (activeEmployees.isEmpty()) throw new AttendanceException("No active employees found");

        AttendanceStatus defaultStatus = determineDefaultStatus(attendanceDate);
        Set<String> alreadyMarked = attendanceRepository.findByAttendanceDate(attendanceDate)
                .stream().map(DailyAttendanceDetail::getEmployeeId).collect(Collectors.toSet());

        List<DailyAttendanceDetail> batch = new ArrayList<>();
        int presentMarked = 0, weekOffMarked = 0, holidayMarked = 0;

        for (UserDetails emp : activeEmployees) {
            if (alreadyMarked.contains(emp.getUserId())) continue;
            DailyAttendanceDetail rec = new DailyAttendanceDetail();
            rec.setEmployeeId(emp.getUserId());
            rec.setAttendanceCycle(cycle);
            rec.setAttendanceDate(attendanceDate);
            rec.setStatus(defaultStatus);
            rec.setMarkedBy(markedBy);
            batch.add(rec);
            switch (defaultStatus) {
                case P  -> presentMarked++;
                case WO -> weekOffMarked++;
                case PH -> holidayMarked++;
                default -> {}
            }
        }

        if (!batch.isEmpty()) attendanceRepository.saveAll(batch);
        updateAttendanceSummary(cycle.getCycleId());

        BulkAttendanceResponseDTO response = new BulkAttendanceResponseDTO();
        response.setSuccess(true);
        response.setMessage("Attendance generated successfully");
        response.setAttendanceDate(attendanceDate);
        response.setTotalEmployees(activeEmployees.size());
        response.setPresentMarked(presentMarked);
        response.setWeekOffMarked(weekOffMarked);
        response.setHolidayMarked(holidayMarked);
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        return response;
    }

    @Transactional
    public void generateMonthlyAttendance(Long cycleId, String generatedBy) {
        long startTime = System.currentTimeMillis();
        AttendanceCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new CycleNotFoundException("Cycle not found: " + cycleId));
        if ("CLOSED".equals(cycle.getStatus())) {
            throw new AttendanceException("Cannot generate attendance for a closed cycle");
        }

        List<UserDetails> employees = getActiveINEmployees();
        if (employees.isEmpty()) { log.warn("No active employees – skipping for cycle {}", cycleId); return; }

        Set<String> existing = attendanceRepository.findAllByCycleId(cycleId).stream()
                .map(a -> a.getEmployeeId() + "_" + a.getAttendanceDate())
                .collect(Collectors.toSet());

        List<LocalDate> holidays =
                holidayRepository.findHolidayDatesBetween(cycle.getStartDate(), cycle.getEndDate());
        Set<LocalDate> holidaySet  = new HashSet<>(holidays);
        Set<Integer>   weekOffDays = getWeekOffDaysSet();

        Map<LocalDate, AttendanceStatus> dateStatusMap = new LinkedHashMap<>();
        for (LocalDate d = cycle.getStartDate(); !d.isAfter(cycle.getEndDate()); d = d.plusDays(1)) {
            dateStatusMap.put(d, determineStatusForDate(d, holidaySet, weekOffDays));
        }

        List<DailyAttendanceDetail> batch = new ArrayList<>();
        for (UserDetails emp : employees) {
            for (Map.Entry<LocalDate, AttendanceStatus> entry : dateStatusMap.entrySet()) {
                if (!existing.contains(emp.getUserId() + "_" + entry.getKey())) {
                    DailyAttendanceDetail rec = new DailyAttendanceDetail();
                    rec.setEmployeeId(emp.getUserId());
                    rec.setAttendanceCycle(cycle);
                    rec.setAttendanceDate(entry.getKey());
                    rec.setStatus(entry.getValue());
                    rec.setMarkedBy(generatedBy);
                    batch.add(rec);
                }
            }
        }

        if (!batch.isEmpty()) {
            int chunk = 500;
            for (int i = 0; i < batch.size(); i += chunk)
                attendanceRepository.saveAll(batch.subList(i, Math.min(i + chunk, batch.size())));
        }

        updateAttendanceSummary(cycleId);
        log.info("generateMonthlyAttendance: {} records for cycle {} in {}ms",
                batch.size(), cycleId, System.currentTimeMillis() - startTime);
    }

    private AttendanceStatus determineStatusForDate(LocalDate date,
                                                    Set<LocalDate> holidaySet,
                                                    Set<Integer> weekOffDays) {
        if (holidaySet.contains(date))                                return AttendanceStatus.PH;
        if (weekOffDays.contains(date.getDayOfWeek().getValue()))     return AttendanceStatus.WO;
        return AttendanceStatus.P;
    }

    private AttendanceStatus determineDefaultStatus(LocalDate date) {
        if (holidayRepository.existsByHolidayDate(date)) return AttendanceStatus.PH;
        if (isWeekOff(date))                             return AttendanceStatus.WO;
        return AttendanceStatus.P;
    }

    private boolean isWeekOff(LocalDate date) {
        return getWeekOffDaysSet().contains(date.getDayOfWeek().getValue());
    }

    private AttendanceCycle getOrCreateCycleForDate(LocalDate date) {
        return cycleRepository.findByDate(date).orElseGet(() -> {
            LocalDate start = calculateCycleStartDate(date);
            LocalDate end   = calculateCycleEndDate(date);
            AttendanceCycle c = new AttendanceCycle();
            c.setStartDate(start); c.setEndDate(end);
            c.setAttendanceMonth(end.getMonth().toString());
            c.setAttendanceYear(end.getYear());
            c.setTotalDaysInCycle((int) (end.toEpochDay() - start.toEpochDay() + 1));
            c.setStatus("OPEN");
            calculateCycleMetrics(c);
            return cycleRepository.save(c);
        });
    }

    private LocalDate calculateCycleStartDate(LocalDate date) {
        return date.getDayOfMonth() >= 26
                ? LocalDate.of(date.getYear(), date.getMonth(), 26)
                : LocalDate.of(date.getYear(), date.getMonth().minus(1), 26);
    }

    private LocalDate calculateCycleEndDate(LocalDate date) {
        return calculateCycleStartDate(date).plusMonths(1).withDayOfMonth(25);
    }

    // ==================== ATTENDANCE OPERATIONS ====================

    @Transactional
    public DailyAttendanceDetail updateAttendance(MarkAttendanceRequestDTO request, String markedBy) {
        long startTime = System.currentTimeMillis();

        AttendanceStatus requestedStatus;
        try {
            requestedStatus = request.getStatus();
        } catch (IllegalArgumentException e) {
            throw new InvalidAttendanceStatusException(
                    "Invalid status: " + request.getStatus() +
                            ". Valid: P, WO, PH, CL, SL, LOP, HD, WFH, SP");
        }

        DailyAttendanceDetail attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(request.getEmployeeId(), request.getAttendanceDate())
                .orElseThrow(() -> new AttendanceRecordNotFoundException(
                        "Attendance not found for " + request.getEmployeeId()
                                + " on " + request.getAttendanceDate()));


        if (attendance.getStatus() == AttendanceStatus.PH && requestedStatus != AttendanceStatus.PH)
            throw new AttendanceException("Cannot change public-holiday status. Please contact HR.");

        if (attendance.getStatus() == requestedStatus) {
            if (request.getRemarks() != null && !request.getRemarks().equals(attendance.getRemarks())) {
                attendance.setRemarks(request.getRemarks());
                attendance.setMarkedBy(markedBy);
                attendanceRepository.save(attendance);
            }
            return attendance;
        }

        if (leavePolicyService.isLeaveStatus(requestedStatus)) {
            try {
                requestedStatus = leavePolicyService.validateAndAdjustLeaveStatus(
                        request.getEmployeeId(), request.getAttendanceDate(),
                        requestedStatus, attendance.getAttendanceCycle().getCycleId());
            } catch (LeaveQuotaExceededException | SandwichLeaveException | ProbationLeaveException e) {
                throw e;
            }
        }

        AttendanceStatus oldStatus = attendance.getStatus();
        attendance.setStatus(requestedStatus);
        attendance.setRemarks(request.getRemarks());
        attendance.setMarkedBy(markedBy);

        DailyAttendanceDetail updated = attendanceRepository.save(attendance);
        updateSingleEmployeeSummary(
                attendance.getAttendanceCycle().getCycleId(),
                request.getEmployeeId(), oldStatus, requestedStatus);

        log.info("Updated attendance for {} on {} in {}ms",
                request.getEmployeeId(), request.getAttendanceDate(),
                System.currentTimeMillis() - startTime);
        return updated;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateSingleEmployeeSummary(Long cycleId, String employeeId,
                                            AttendanceStatus oldStatus, AttendanceStatus newStatus) {
        AttendanceCycle cycle = cycleRepository.findById(cycleId).orElse(null);
        if (cycle == null) { log.warn("Cycle not found: {}", cycleId); return; }

        EmployeeAttendanceSummary summary = summaryRepository
                .findByEmployeeIdAndAttendanceCycle_CycleId(employeeId, cycleId)
                .orElseGet(() -> initializeNewSummary(employeeId, cycle));

        adjustSummaryForStatusChange(summary, oldStatus, newStatus);
        recalculateSummaryTotals(summary);
        summaryRepository.save(summary);
    }

    private EmployeeAttendanceSummary initializeNewSummary(String employeeId, AttendanceCycle cycle) {
        EmployeeAttendanceSummary s = new EmployeeAttendanceSummary();
        s.setEmployeeId(employeeId); s.setAttendanceCycle(cycle);
        s.setTotalWorkingDays(cycle.getTotalWorkingDays());
        s.setTotalWeekOffs(cycle.getTotalWeekOffs());
        s.setTotalPublicHolidays(cycle.getTotalPublicHolidays());
        s.setCasualLeaves(0); s.setSickLeaves(0); s.setLossOfPayLeaves(0); s.setSpecialLeaves(0);
        s.setTotalWorkedDays(0); s.setTotalLeavesTaken(0);
        s.setTotalPayDays(cycle.getTotalWeekOffs() + cycle.getTotalPublicHolidays());
        return s;
    }

    private void adjustSummaryForStatusChange(EmployeeAttendanceSummary s,
                                              AttendanceStatus oldStatus, AttendanceStatus newStatus) {
        decrementStatusCount(s, oldStatus);
        incrementStatusCount(s, newStatus);
    }

    private void decrementStatusCount(EmployeeAttendanceSummary s, AttendanceStatus status) {
        if (status == null) return;
        switch (status) {
            case P, WFH, HD -> s.setTotalWorkedDays(Math.max(0, s.getTotalWorkedDays() - 1));
            case CL          -> s.setCasualLeaves(Math.max(0, s.getCasualLeaves() - 1));
            case SL          -> s.setSickLeaves(Math.max(0, s.getSickLeaves() - 1));
            case SP          -> s.setSpecialLeaves(Math.max(0, s.getSpecialLeaves() - 1));
            case LOP         -> s.setLossOfPayLeaves(Math.max(0, s.getLossOfPayLeaves() - 1));
            default          -> {}
        }
    }

    private void incrementStatusCount(EmployeeAttendanceSummary s, AttendanceStatus status) {
        if (status == null) return;
        switch (status) {
            case P, WFH, HD -> s.setTotalWorkedDays(s.getTotalWorkedDays() + 1);
            case CL          -> s.setCasualLeaves(s.getCasualLeaves() + 1);
            case SL          -> s.setSickLeaves(s.getSickLeaves() + 1);
            case SP          -> s.setSpecialLeaves(s.getSpecialLeaves() + 1);
            case LOP         -> s.setLossOfPayLeaves(s.getLossOfPayLeaves() + 1);
            default          -> {}
        }
    }

    private void recalculateSummaryTotals(EmployeeAttendanceSummary s) {
        int totalLeaves = s.getCasualLeaves() + s.getSickLeaves()
                + s.getLossOfPayLeaves() + s.getSpecialLeaves();
        s.setTotalLeavesTaken(totalLeaves);
        int payDays = s.getTotalWorkedDays() + s.getTotalWeekOffs()
                + s.getTotalPublicHolidays() + s.getCasualLeaves()
                + s.getSickLeaves() + s.getSpecialLeaves();
        s.setTotalPayDays(payDays);
    }

    @Transactional
    public void updateAttendanceSummary(Long cycleId) {
        AttendanceCycle cycle = cycleRepository.findById(cycleId).orElse(null);
        if (cycle == null) { log.warn("Cycle not found: {}", cycleId); return; }

        List<UserDetails> employees = getActiveINEmployees();
        Map<String, List<DailyAttendanceDetail>> byEmployee =
                attendanceRepository.findAllByCycleId(cycleId).stream()
                        .collect(Collectors.groupingBy(DailyAttendanceDetail::getEmployeeId));
        Map<String, EmployeeAttendanceSummary> existingSummaries =
                summaryRepository.findByAttendanceCycle_CycleId(cycleId).stream()
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

            summary.setTotalWorkingDays(cycle.getTotalWorkingDays());
            summary.setTotalWeekOffs(cycle.getTotalWeekOffs());
            summary.setTotalPublicHolidays(cycle.getTotalPublicHolidays());
            summary.setCasualLeaves(cnt.getOrDefault(AttendanceStatus.CL,  0L).intValue());
            summary.setSickLeaves(cnt.getOrDefault(AttendanceStatus.SL,    0L).intValue());
            summary.setLossOfPayLeaves(cnt.getOrDefault(AttendanceStatus.LOP, 0L).intValue());
            summary.setSpecialLeaves(cnt.getOrDefault(AttendanceStatus.SP,  0L).intValue());
            summary.setTotalLeavesTaken(summary.getCasualLeaves() + summary.getSickLeaves()
                    + summary.getLossOfPayLeaves() + summary.getSpecialLeaves());

            double worked = cnt.getOrDefault(AttendanceStatus.P, 0L)
                    + cnt.getOrDefault(AttendanceStatus.WFH, 0L)
                    + cnt.getOrDefault(AttendanceStatus.HD, 0L) * 0.5;
            summary.setTotalWorkedDays((int) Math.floor(worked));

            double payDays = worked
                    + cnt.getOrDefault(AttendanceStatus.WO, 0L)
                    + cnt.getOrDefault(AttendanceStatus.PH, 0L)
                    + cnt.getOrDefault(AttendanceStatus.CL, 0L)
                    + cnt.getOrDefault(AttendanceStatus.SL, 0L)
                    + cnt.getOrDefault(AttendanceStatus.SP, 0L);
            summary.setTotalPayDays((int) Math.floor(payDays));
            toSave.add(summary);
        }

        if (!toSave.isEmpty()) summaryRepository.saveAll(toSave);
        log.info("Updated summaries for {} employees in cycle {}", toSave.size(), cycleId);
    }

    // ==================== ATTENDANCE QUERIES ====================

    public List<AttendanceGridResponseDTO> getAttendanceGrid(String employeeId, Long cycleId) {
        AttendanceCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new CycleNotFoundException("Cycle not found: " + cycleId));
        UserDetails employee = getEmployeeById(employeeId);
        if (employee == null) throw new EmployeeNotFoundException("Employee not found: " + employeeId);

        Map<LocalDate, DailyAttendanceDetail> attendanceMap =
                attendanceRepository.findByCycleIdAndEmployeeId(cycleId, employeeId).stream()
                        .collect(Collectors.toMap(DailyAttendanceDetail::getAttendanceDate, a -> a));

        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEEE");
        List<AttendanceGridResponseDTO> grid = new ArrayList<>();
        for (LocalDate d = cycle.getStartDate(); !d.isAfter(cycle.getEndDate()); d = d.plusDays(1)) {
            DailyAttendanceDetail rec = attendanceMap.get(d);
            AttendanceStatus status   = rec != null ? rec.getStatus() : AttendanceStatus.P;
            AttendanceGridResponseDTO dto = new AttendanceGridResponseDTO();
            dto.setDate(d); dto.setDay(d.format(dayFmt));
            dto.setStatus(status.name()); dto.setStatusName(status.getDescription());
            dto.setRemarks(rec != null ? rec.getRemarks() : null);
            dto.setEmployeeName(employee.getUserName()); dto.setDesignation(employee.getDesignation());
            grid.add(dto);
        }
        return grid;
    }

    public AttendanceSummaryResponseDTO getAttendanceSummary(String employeeId, Long cycleId) {
        EmployeeAttendanceSummary summary = summaryRepository
                .findByEmployeeIdAndAttendanceCycle_CycleId(employeeId, cycleId)
                .orElseThrow(() -> new SummaryNotFoundException(
                        "Summary not found for " + employeeId + " in cycle " + cycleId));

        AttendanceCycle cycle    = summary.getAttendanceCycle();
        UserDetails     employee = getEmployeeById(employeeId);
        if (employee == null) throw new EmployeeNotFoundException("Employee not found: " + employeeId);

        AttendanceSummaryResponseDTO dto = new AttendanceSummaryResponseDTO();
        dto.setEmployeeId(employeeId); dto.setEmployeeName(employee.getUserName());
        dto.setDesignation(employee.getDesignation()); dto.setDepartment(employee.getDepartment());
        dto.setAttendanceMonth(cycle.getAttendanceMonth()); dto.setAttendanceYear(cycle.getAttendanceYear());
        dto.setTotalWorkingDays(summary.getTotalWorkingDays());
        dto.setTotalWeekOffs(summary.getTotalWeekOffs());
        dto.setTotalPublicHolidays(summary.getTotalPublicHolidays());
        dto.setCasualLeaves(summary.getCasualLeaves());
        dto.setSickLeaves(summary.getSickLeaves());
        dto.setLossOfPayLeaves(summary.getLossOfPayLeaves());
        dto.setSpecialLeaves(summary.getSpecialLeaves());
        dto.setTotalLeavesTaken(summary.getTotalLeavesTaken());
        dto.setTotalWorkedDays(summary.getTotalWorkedDays());
        dto.setTotalPayDays(summary.getTotalPayDays());
        if (summary.getTotalWorkingDays() != null && summary.getTotalWorkingDays() > 0) {
            double pct = (summary.getTotalWorkedDays() * 100.0) / summary.getTotalWorkingDays();
            dto.setAttendancePercentage(Math.round(pct * 100.0) / 100.0);
        } else {
            dto.setAttendancePercentage(0.0);
        }
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        breakdown.put("Casual Leave (CL)",  summary.getCasualLeaves());
        breakdown.put("Sick Leave (SL)",     summary.getSickLeaves());
        breakdown.put("Special Leave (SP)",  summary.getSpecialLeaves());
        breakdown.put("Loss of Pay (LOP)",   summary.getLossOfPayLeaves());
        dto.setLeaveBreakdown(breakdown);
        return dto;
    }

    public PaginatedResponseDTO<AttendanceGridResponseDTO> getMonthlyAttendance(MonthlyAttendanceRequestDTO request) {
        Pageable pageable = PageRequest.of(
                request.getPage(), request.getSize(),
                Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy()));

        AttendanceCycle cycle = cycleRepository.findByAttendanceYearAndAttendanceMonth(
                        request.getYear(), request.getMonth().toUpperCase())
                .orElseThrow(() -> new CycleNotFoundException(
                        "Cycle not found for " + request.getMonth() + " " + request.getYear()));

        Page<DailyAttendanceDetail> page = attendanceRepository.findByCycleId(cycle.getCycleId(), pageable);
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEEE");

        List<AttendanceGridResponseDTO> content = page.getContent().stream().map(a -> {
            UserDetails emp = getEmployeeById(a.getEmployeeId());
            AttendanceGridResponseDTO dto = new AttendanceGridResponseDTO();
            dto.setDate(a.getAttendanceDate()); dto.setDay(a.getAttendanceDate().format(dayFmt));
            dto.setStatus(a.getStatus().name()); dto.setStatusName(a.getStatus().getDescription());
            dto.setRemarks(a.getRemarks());
            dto.setEmployeeName(emp != null ? emp.getUserName() : "Unknown");
            dto.setDesignation(emp != null ? emp.getDesignation() : "Unknown");
            return dto;
        }).collect(Collectors.toList());

        PaginatedResponseDTO<AttendanceGridResponseDTO> response = new PaginatedResponseDTO<>();
        response.setContent(content); response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize()); response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages()); response.setLast(page.isLast());
        return response;
    }

    public List<AttendanceGridResponseDTO> getAttendanceByDateRange(
            String employeeId, LocalDate startDate, LocalDate endDate) {
        UserDetails emp = getEmployeeById(employeeId);
        if (emp == null) throw new EmployeeNotFoundException("Employee not found: " + employeeId);
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEEE");
        return attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(employeeId, startDate, endDate)
                .stream().map(a -> {
                    AttendanceGridResponseDTO dto = new AttendanceGridResponseDTO();
                    dto.setDate(a.getAttendanceDate()); dto.setDay(a.getAttendanceDate().format(dayFmt));
                    dto.setStatus(a.getStatus().name()); dto.setStatusName(a.getStatus().getDescription());
                    dto.setRemarks(a.getRemarks());
                    dto.setEmployeeName(emp.getUserName()); dto.setDesignation(emp.getDesignation());
                    return dto;
                }).collect(Collectors.toList());
    }

    public Map<String, Object> getAttendanceStatistics(Long cycleId) {
        AttendanceCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new CycleNotFoundException("Cycle not found: " + cycleId));
        List<EmployeeAttendanceSummary> summaries =
                summaryRepository.findByAttendanceCycle_CycleId(cycleId);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("cycleId", cycle.getCycleId()); stats.put("cycleMonth", cycle.getAttendanceMonth());
        stats.put("cycleYear", cycle.getAttendanceYear()); stats.put("cycleStatus", cycle.getStatus());
        stats.put("totalEmployees", summaries.size());
        stats.put("totalPresentDays",  summaries.stream().mapToInt(EmployeeAttendanceSummary::getTotalWorkedDays).sum());
        stats.put("totalLeavesTaken",  summaries.stream().mapToInt(EmployeeAttendanceSummary::getTotalLeavesTaken).sum());
        double avgAtt = summaries.stream()
                .filter(s -> s.getTotalWorkingDays() != null && s.getTotalWorkingDays() > 0)
                .mapToDouble(s -> (s.getTotalWorkedDays() * 100.0) / s.getTotalWorkingDays())
                .average().orElse(0);
        stats.put("averageAttendancePercentage", Math.round(avgAtt * 100.0) / 100.0);
        Map<String, Integer> leaveSummary = new LinkedHashMap<>();
        leaveSummary.put("Casual Leave (CL)",  summaries.stream().mapToInt(EmployeeAttendanceSummary::getCasualLeaves).sum());
        leaveSummary.put("Sick Leave (SL)",    summaries.stream().mapToInt(EmployeeAttendanceSummary::getSickLeaves).sum());
        leaveSummary.put("Special Leave (SP)", summaries.stream().mapToInt(EmployeeAttendanceSummary::getSpecialLeaves).sum());
        leaveSummary.put("Loss of Pay (LOP)",  summaries.stream().mapToInt(EmployeeAttendanceSummary::getLossOfPayLeaves).sum());
        stats.put("leaveSummary", leaveSummary);
        return stats;
    }

    @Transactional
    public void processBulkAttendanceUpdate(List<MarkAttendanceRequestDTO> requests, String userId) {
        int success = 0, failure = 0;
        for (MarkAttendanceRequestDTO req : requests) {
            try { updateAttendance(req, userId); success++; }
            catch (Exception e) { failure++; log.error("Bulk update failed for {} on {}: {}", req.getEmployeeId(), req.getAttendanceDate(), e.getMessage()); }
        }
        log.info("Bulk update done. Success: {}, Failure: {}", success, failure);
        if (failure > 0 && success == 0)
            throw new BulkAttendanceException("Bulk attendance update failed for all records");
    }

    // ==================== HELPERS ====================

    private List<UserDetails> getActiveINEmployees() {
        return userDao.findAllActiveInEmployeesExcludingExternal();
    }

    private UserDetails getEmployeeById(String userId) {
        return userDao.findByUserId(userId);
    }

    private CycleResponseDTO convertToCycleResponseDTO(AttendanceCycle c) {
        if (c == null) return null;
        CycleResponseDTO dto = new CycleResponseDTO();
        dto.setCycleId(c.getCycleId()); dto.setAttendanceMonth(c.getAttendanceMonth());
        dto.setAttendanceYear(c.getAttendanceYear()); dto.setStartDate(c.getStartDate());
        dto.setEndDate(c.getEndDate()); dto.setTotalDaysInCycle(c.getTotalDaysInCycle());
        dto.setTotalWorkingDays(c.getTotalWorkingDays()); dto.setTotalWeekOffs(c.getTotalWeekOffs());
        dto.setTotalPublicHolidays(c.getTotalPublicHolidays()); dto.setStatus(c.getStatus());
        dto.setCreatedAt(c.getCreatedAt()); dto.setUpdatedAt(c.getUpdatedAt());
        dto.setCreatedBy(c.getCreatedBy()); dto.setUpdatedBy(c.getUpdatedBy());
        return dto;
    }
}