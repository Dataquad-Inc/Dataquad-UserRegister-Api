package com.dataquadinc.service;

import com.dataquadinc.exceptions.LeaveQuotaExceededException;
import com.dataquadinc.exceptions.ProbationLeaveException;
import com.dataquadinc.exceptions.SandwichLeaveException;
import com.dataquadinc.model.AttendanceCycle;
import com.dataquadinc.model.DailyAttendanceDetail;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.repository.AttendanceCycleRepository;
import com.dataquadinc.repository.DailyAttendanceRepository;
import com.dataquadinc.repository.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LeavePolicyService {

    private static final Logger log = LoggerFactory.getLogger(LeavePolicyService.class);

    private final DailyAttendanceRepository attendanceRepository;
    private final AttendanceCycleRepository cycleRepository;
    private final UserDao userDao;

    // Leave quotas for non-probation employees
    private static final int MAX_SICK_LEAVES_PER_CYCLE    = 1;
    private static final int MAX_CASUAL_LEAVES_PER_CYCLE  = 1;
    // Special Leave (SP) has NO quota limit – always unlimited

    public LeavePolicyService(DailyAttendanceRepository attendanceRepository,
                              AttendanceCycleRepository cycleRepository,
                              UserDao userDao) {
        this.attendanceRepository = attendanceRepository;
        this.cycleRepository      = cycleRepository;
        this.userDao              = userDao;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SANDWICH POLICY RULES
    //
    //  Any leave (CL / SL / SP / LOP / probation-converted-LOP) on FRIDAY or
    //  MONDAY triggers the sandwich rule:
    //
    //    FRIDAY leave  → mark Friday + Saturday + Sunday as LOP  (3 days total)
    //    MONDAY leave  → mark Saturday + Sunday + Monday as LOP  (3 days total)
    //
    //  The original leave day is INCLUDED in the 3-day block, so the "extra"
    //  days added are always 2 (the surrounding weekend days).
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates and adjusts leave status based on company policies.
     *
     * <pre>
     * Rules for ALL employees:
     *
     * 1. SPECIAL LEAVE (SP) – always paid, unlimited; still subject to sandwich.
     * 2. FRIDAY / MONDAY leaves (ALL employees):
     *      FRIDAY leave  → 3 days LOP: Fri + Sat + Sun
     *      MONDAY leave  → 3 days LOP: Sat + Sun + Mon
     * 3. PROBATION employees – only SP allowed; every other leave → LOP
     *      (+ sandwich expansion if Fri/Mon)
     * 4. NON-PROBATION – 1 CL OR 1 SL per cycle (not both); SP unlimited.
     * </pre>
     */
    @Transactional
    public DailyAttendanceDetail.AttendanceStatus validateAndAdjustLeaveStatus(
            String employeeId,
            LocalDate leaveDate,
            DailyAttendanceDetail.AttendanceStatus requestedStatus,
            Long cycleId) throws SandwichLeaveException, ProbationLeaveException, LeaveQuotaExceededException {

        UserDetails employee = userDao.findByUserId(employeeId);
        if (employee == null) {
            throw new RuntimeException("Employee not found: " + employeeId);
        }

        if (!isLeaveStatus(requestedStatus)) {
            return requestedStatus;
        }

        boolean isOnProbation = "YES".equalsIgnoreCase(employee.getProbation());

        // ── SPECIAL LEAVE (SP) ──────────────────────────────────────────────
        if (requestedStatus == DailyAttendanceDetail.AttendanceStatus.SP) {
            log.info("SP requested for employee {}.", employeeId);

            if (isFridayOrMonday(leaveDate)) {
                List<LocalDate> lopDays = getSandwichDays(leaveDate);
                createOrUpdateLOPRecords(employeeId, lopDays, cycleId);
                throw new SandwichLeaveException(buildSandwichMessage("Special Leave", leaveDate, lopDays));
            }

            return requestedStatus; // SP on other days – approved, paid
        }

        // ── PROBATION CHECK (non-SP leaves) ─────────────────────────────────
        if (isOnProbation) {
            log.info("Employee {} is on probation; converting {} to LOP.", employeeId, requestedStatus);

            if (isFridayOrMonday(leaveDate)) {
                List<LocalDate> lopDays = getSandwichDays(leaveDate);
                createOrUpdateLOPRecords(employeeId, lopDays, cycleId);
                throw new ProbationLeaveException(
                        String.format("Employee is on probation. Leave on %s triggers sandwich policy. " +
                                "3 days LOP created: %s", leaveDate.getDayOfWeek(), lopDays));
            } else {
                createOrUpdateLOPRecord(employeeId, leaveDate, cycleId);
                throw new ProbationLeaveException(
                        String.format("Employee is on probation; %s converted to LOP.", requestedStatus));
            }
        }

        // ── NON-PROBATION: quota check (CL / SL) ────────────────────────────
        if (isPaidLeaveStatus(requestedStatus)) {
            validatePaidLeaveQuota(employeeId, requestedStatus, leaveDate, cycleId);
        }

        // ── SANDWICH POLICY (CL / SL on Fri or Mon) ─────────────────────────
        if (requestedStatus == DailyAttendanceDetail.AttendanceStatus.CL ||
                requestedStatus == DailyAttendanceDetail.AttendanceStatus.SL) {

            if (isFridayOrMonday(leaveDate)) {
                List<LocalDate> lopDays = getSandwichDays(leaveDate);
                createOrUpdateLOPRecords(employeeId, lopDays, cycleId);
                throw new SandwichLeaveException(buildSandwichMessage(requestedStatus.name(), leaveDate, lopDays));
            }
        }

        // ── LOP directly requested on Fri / Mon ─────────────────────────────
        if (requestedStatus == DailyAttendanceDetail.AttendanceStatus.LOP) {
            if (isFridayOrMonday(leaveDate)) {
                List<LocalDate> lopDays = getSandwichDays(leaveDate);
                createOrUpdateLOPRecords(employeeId, lopDays, cycleId);
                throw new SandwichLeaveException(buildSandwichMessage("LOP", leaveDate, lopDays));
            }
        }

        return requestedStatus;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isFridayOrMonday(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.FRIDAY || d == DayOfWeek.MONDAY;
    }

    /**
     * Returns the 3-day sandwich block that must all be marked LOP.
     *
     * <ul>
     *   <li>FRIDAY  → [ Friday, Saturday, Sunday ]</li>
     *   <li>MONDAY  → [ Saturday, Sunday, Monday ]</li>
     * </ul>
     *
     * The original leave day is <em>included</em> in the list so callers can
     * simply call {@link #createOrUpdateLOPRecords} with the full list.
     */
    public List<LocalDate> getSandwichDays(LocalDate leaveDate) {
        List<LocalDate> days = new ArrayList<>();
        DayOfWeek dow = leaveDate.getDayOfWeek();

        if (dow == DayOfWeek.FRIDAY) {
            days.add(leaveDate);                   // Friday  (original leave day)
            days.add(leaveDate.plusDays(1));        // Saturday
            days.add(leaveDate.plusDays(2));        // Sunday
            log.info("Sandwich block (Friday): {} → {}", days.get(0), days.get(2));
        } else if (dow == DayOfWeek.MONDAY) {
            days.add(leaveDate.minusDays(2));       // Saturday
            days.add(leaveDate.minusDays(1));       // Sunday
            days.add(leaveDate);                   // Monday  (original leave day)
            log.info("Sandwich block (Monday): {} → {}", days.get(0), days.get(2));
        }
        return days;
    }

    private String buildSandwichMessage(String leaveType, LocalDate leaveDate, List<LocalDate> lopDays) {
        return String.format(
                "Sandwich policy: %s on %s (%s). 3 LOP days created: %s, %s, %s.",
                leaveType, leaveDate, leaveDate.getDayOfWeek(),
                lopDays.get(0), lopDays.get(1), lopDays.get(2));
    }

    @Transactional
    private void createOrUpdateLOPRecords(String employeeId, List<LocalDate> lopDays, Long cycleId) {
        for (LocalDate date : lopDays) {
            createOrUpdateLOPRecord(employeeId, date, cycleId);
        }
        log.info("Created/updated {} LOP records for employee {}", lopDays.size(), employeeId);
    }

    @Transactional
    private void createOrUpdateLOPRecord(String employeeId, LocalDate date, Long cycleId) {
        Optional<DailyAttendanceDetail> opt =
                attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, date);

        if (opt.isPresent()) {
            DailyAttendanceDetail rec = opt.get();
            if (rec.getStatus() != DailyAttendanceDetail.AttendanceStatus.LOP) {
                DailyAttendanceDetail.AttendanceStatus old = rec.getStatus();
                rec.setStatus(DailyAttendanceDetail.AttendanceStatus.LOP);
                rec.setMarkedBy("SYSTEM");
                attendanceRepository.save(rec);
                log.info("Updated {} on {} → LOP (was {})", employeeId, date, old);
            }
        } else {
            DailyAttendanceDetail rec = new DailyAttendanceDetail();
            rec.setEmployeeId(employeeId);
            rec.setAttendanceDate(date);
            rec.setStatus(DailyAttendanceDetail.AttendanceStatus.LOP);
            rec.setMarkedBy("SYSTEM");

            if (cycleId != null) {
                cycleRepository.findById(cycleId).ifPresent(rec::setAttendanceCycle);
            }

            attendanceRepository.save(rec);
            log.info("Created new LOP record for {} on {}", employeeId, date);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  QUOTA VALIDATION (non-probation CL / SL)
    // ─────────────────────────────────────────────────────────────────────────

    private void validatePaidLeaveQuota(String employeeId,
                                        DailyAttendanceDetail.AttendanceStatus requestedStatus,
                                        LocalDate leaveDate,
                                        Long cycleId) throws LeaveQuotaExceededException {

        // SP has no quota – guard should never reach here for SP, but just in case:
        if (requestedStatus == DailyAttendanceDetail.AttendanceStatus.SP) return;

        List<DailyAttendanceDetail> existingPaidLeaves = attendanceRepository
                .findByCycleIdAndEmployeeId(cycleId, employeeId)
                .stream()
                .filter(a -> isPaidLeaveStatus(a.getStatus()))
                .filter(a -> a.getStatus() != DailyAttendanceDetail.AttendanceStatus.SP)
                .filter(a -> !a.getAttendanceDate().equals(leaveDate))
                .toList();

        if (existingPaidLeaves.isEmpty()) return;

        if (requestedStatus == DailyAttendanceDetail.AttendanceStatus.CL) {
            long existingCL = existingPaidLeaves.stream()
                    .filter(a -> a.getStatus() == DailyAttendanceDetail.AttendanceStatus.CL)
                    .count();
            if (existingCL >= MAX_CASUAL_LEAVES_PER_CYCLE) {
                throw new LeaveQuotaExceededException(
                        String.format("CL quota exhausted. Only %d CL allowed per cycle.",
                                MAX_CASUAL_LEAVES_PER_CYCLE));
            }
            boolean hasSL = existingPaidLeaves.stream()
                    .anyMatch(a -> a.getStatus() == DailyAttendanceDetail.AttendanceStatus.SL);
            if (hasSL) {
                throw new LeaveQuotaExceededException(
                        "SL already taken this cycle. Cannot add CL (only one of CL/SL allowed per cycle).");
            }
        }

        if (requestedStatus == DailyAttendanceDetail.AttendanceStatus.SL) {
            long existingSL = existingPaidLeaves.stream()
                    .filter(a -> a.getStatus() == DailyAttendanceDetail.AttendanceStatus.SL)
                    .count();
            if (existingSL >= MAX_SICK_LEAVES_PER_CYCLE) {
                throw new LeaveQuotaExceededException(
                        String.format("SL quota exhausted. Only %d SL allowed per cycle.",
                                MAX_SICK_LEAVES_PER_CYCLE));
            }
            boolean hasCL = existingPaidLeaves.stream()
                    .anyMatch(a -> a.getStatus() == DailyAttendanceDetail.AttendanceStatus.CL);
            if (hasCL) {
                throw new LeaveQuotaExceededException(
                        "CL already taken this cycle. Cannot add SL (only one of CL/SL allowed per cycle).");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STATUS UTILITY METHODS
    // ─────────────────────────────────────────────────────────────────────────

    public boolean isLeaveStatus(DailyAttendanceDetail.AttendanceStatus status) {
        return status == DailyAttendanceDetail.AttendanceStatus.CL  ||
                status == DailyAttendanceDetail.AttendanceStatus.SL  ||
                status == DailyAttendanceDetail.AttendanceStatus.LOP ||
                status == DailyAttendanceDetail.AttendanceStatus.SP;
    }

    /** CL, SL, and SP are all paid leave types. */
    public boolean isPaidLeaveStatus(DailyAttendanceDetail.AttendanceStatus status) {
        return status == DailyAttendanceDetail.AttendanceStatus.CL  ||
                status == DailyAttendanceDetail.AttendanceStatus.SL  ||
                status == DailyAttendanceDetail.AttendanceStatus.SP;
    }

    public boolean isOnProbation(String employeeId) {
        UserDetails emp = userDao.findByUserId(employeeId);
        return emp != null && "YES".equalsIgnoreCase(emp.getProbation());
    }

    public int getPaidLeaveCount(String employeeId, Long cycleId) {
        return (int) attendanceRepository.findByCycleIdAndEmployeeId(cycleId, employeeId)
                .stream()
                .filter(a -> isPaidLeaveStatus(a.getStatus()))
                .filter(a -> a.getStatus() != DailyAttendanceDetail.AttendanceStatus.SP)
                .count();
    }

    public int getSpecialLeaveCount(String employeeId, Long cycleId) {
        return (int) attendanceRepository.findByCycleIdAndEmployeeId(cycleId, employeeId)
                .stream()
                .filter(a -> a.getStatus() == DailyAttendanceDetail.AttendanceStatus.SP)
                .count();
    }

    public LeaveDetailsDTO getLeaveDetails(String employeeId, Long cycleId) {
        List<DailyAttendanceDetail> attendances =
                attendanceRepository.findByCycleIdAndEmployeeId(cycleId, employeeId);

        LeaveDetailsDTO details = new LeaveDetailsDTO();
        details.setEmployeeId(employeeId);
        details.setOnProbation(isOnProbation(employeeId));

        int cl = 0, sl = 0, sp = 0, lop = 0;
        for (DailyAttendanceDetail a : attendances) {
            if (isLeaveStatus(a.getStatus())) {
                details.addLeave(a.getAttendanceDate(), a.getStatus());
                switch (a.getStatus()) {
                    case CL  -> cl++;
                    case SL  -> sl++;
                    case SP  -> sp++;
                    case LOP -> lop++;
                    default  -> {}
                }
            }
        }
        details.setClCount(cl);
        details.setSlCount(sl);
        details.setSpCount(sp);
        details.setLopCount(lop);
        return details;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INNER DTOs
    // ─────────────────────────────────────────────────────────────────────────

    public static class LeaveDetailsDTO {
        private String employeeId;
        private boolean onProbation;
        private List<LeaveEntry> leaves = new ArrayList<>();
        private int clCount, slCount, spCount, lopCount;

        public String getEmployeeId()               { return employeeId; }
        public void   setEmployeeId(String v)       { this.employeeId = v; }

        public boolean isOnProbation()              { return onProbation; }
        public void    setOnProbation(boolean v)    { this.onProbation = v; }

        public List<LeaveEntry> getLeaves()         { return leaves; }
        public void setLeaves(List<LeaveEntry> v)   { this.leaves = v; }

        public int getClCount()                     { return clCount; }
        public void setClCount(int v)               { this.clCount = v; }

        public int getSlCount()                     { return slCount; }
        public void setSlCount(int v)               { this.slCount = v; }

        public int getSpCount()                     { return spCount; }
        public void setSpCount(int v)               { this.spCount = v; }

        public int getLopCount()                    { return lopCount; }
        public void setLopCount(int v)              { this.lopCount = v; }

        public void addLeave(LocalDate date, DailyAttendanceDetail.AttendanceStatus status) {
            LeaveEntry e = new LeaveEntry();
            e.setDate(date);
            e.setStatus(status);
            leaves.add(e);
        }

        @Override
        public String toString() {
            return String.format("LeaveDetailsDTO{id='%s', probation=%s, CL=%d, SL=%d, SP=%d, LOP=%d}",
                    employeeId, onProbation, clCount, slCount, spCount, lopCount);
        }
    }

    public static class LeaveEntry {
        private LocalDate date;
        private DailyAttendanceDetail.AttendanceStatus status;

        public LocalDate getDate()                              { return date; }
        public void setDate(LocalDate date)                     { this.date = date; }

        public DailyAttendanceDetail.AttendanceStatus getStatus() { return status; }
        public void setStatus(DailyAttendanceDetail.AttendanceStatus status) { this.status = status; }
    }
}