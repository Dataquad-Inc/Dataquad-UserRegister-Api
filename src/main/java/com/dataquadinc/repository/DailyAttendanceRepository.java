package com.dataquadinc.repository;

import com.dataquadinc.model.DailyAttendanceDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAttendanceRepository extends JpaRepository<DailyAttendanceDetail, Long> {

    // ─── Single-record lookups ────────────────────────────────────────────────

    Optional<DailyAttendanceDetail> findByEmployeeIdAndAttendanceDate(
            String employeeId, LocalDate attendanceDate);

    List<DailyAttendanceDetail> findByEmployeeIdAndAttendanceDateBetween(
            String employeeId, LocalDate startDate, LocalDate endDate);

    List<DailyAttendanceDetail> findByAttendanceDate(LocalDate date);

    // ─── Cycle-scoped queries ─────────────────────────────────────────────────

    @Query("""
            SELECT d FROM DailyAttendanceDetail d
            WHERE d.attendanceCycle.cycleId = :cycleId
              AND d.employeeId = :employeeId
            ORDER BY d.attendanceDate
            """)
    List<DailyAttendanceDetail> findByCycleIdAndEmployeeId(
            @Param("cycleId") Long cycleId,
            @Param("employeeId") String employeeId);

    @Query("""
            SELECT d FROM DailyAttendanceDetail d
            WHERE d.attendanceCycle.cycleId = :cycleId
            ORDER BY d.attendanceDate
            """)
    Page<DailyAttendanceDetail> findByCycleId(
            @Param("cycleId") Long cycleId, Pageable pageable);

    @Query("""
            SELECT d FROM DailyAttendanceDetail d
            WHERE d.attendanceCycle.cycleId = :cycleId
            ORDER BY d.employeeId, d.attendanceDate
            """)
    List<DailyAttendanceDetail> findAllByCycleId(@Param("cycleId") Long cycleId);

    @Query("""
            SELECT d FROM DailyAttendanceDetail d
            WHERE d.attendanceCycle.cycleId = :cycleId
              AND d.status IN :statuses
            """)
    List<DailyAttendanceDetail> findByCycleIdAndStatusIn(
            @Param("cycleId") Long cycleId,
            @Param("statuses") List<DailyAttendanceDetail.AttendanceStatus> statuses);

    // ─── Count queries ────────────────────────────────────────────────────────

    @Query("""
            SELECT COUNT(d) FROM DailyAttendanceDetail d
            WHERE d.attendanceCycle.cycleId = :cycleId
            """)
    long countByCycleId(@Param("cycleId") Long cycleId);

    // ─── Projection queries (lightweight – avoids full entity hydration) ──────

    @Query("""
            SELECT d.employeeId, d.attendanceDate, d.status
            FROM   DailyAttendanceDetail d
            WHERE  d.attendanceCycle.cycleId = :cycleId
            """)
    List<Object[]> findAttendanceProjectionByCycleId(@Param("cycleId") Long cycleId);

    @Query(value = """
            SELECT employee_id, attendance_date, status
            FROM   daily_attendance_details
            WHERE  cycle_id = :cycleId
            """, nativeQuery = true)
    List<Object[]> findAttendanceProjectionNative(@Param("cycleId") Long cycleId);

    // ============ NEW OPTIMIZED QUERIES ============

    /**
     * Native projection with employee filtering - for paginated bulk requests
     * Returns [employee_id, attendance_date, status]
     */
    @Query(value = """
            SELECT d.employee_id, d.attendance_date, d.status
            FROM daily_attendance_details d
            WHERE d.cycle_id = :cycleId
            AND d.employee_id IN (:employeeIds)
            ORDER BY d.employee_id, d.attendance_date
            """, nativeQuery = true)
    List<Object[]> findAttendanceProjectionByEmployeeIdsNative(
            @Param("cycleId") Long cycleId,
            @Param("employeeIds") List<String> employeeIds);

    /**
     * Get attendance with employee details in one native query - for exports
     * Returns [emp_id, emp_name, date, status, designation, department]
     */
    @Query(value = """
            SELECT 
                d.employee_id,
                u.user_name as employee_name,
                d.attendance_date,
                d.status,
                u.designation,
                u.department
            FROM daily_attendance_details d
            JOIN user_details u ON d.employee_id = u.user_id
            WHERE d.cycle_id = :cycleId
            ORDER BY u.user_name, d.attendance_date
            """, nativeQuery = true)
    List<Object[]> findAttendanceWithEmployeeDetails(@Param("cycleId") Long cycleId);

    /**
     * Get attendance counts grouped by status for a cycle
     */
    @Query("""
            SELECT d.status, COUNT(d)
            FROM DailyAttendanceDetail d
            WHERE d.attendanceCycle.cycleId = :cycleId
            GROUP BY d.status
            """)
    List<Object[]> getAttendanceStatusCounts(@Param("cycleId") Long cycleId);

    /**
     * Find employees who have no attendance records in the cycle (missing data)
     */
    @Query(value = """
            SELECT DISTINCT u.user_id, u.user_name
            FROM user_details u
            WHERE u.entity = 'IN'
            AND u.status = 'ACTIVE'
            AND u.designation <> 'Candidate'
            AND u.designation <> 'testuser'
            AND u.user_id NOT IN (
                SELECT DISTINCT d.employee_id
                FROM daily_attendance_details d
                WHERE d.cycle_id = :cycleId
            )
            """, nativeQuery = true)
    List<Object[]> findEmployeesWithoutAttendance(@Param("cycleId") Long cycleId);

    /**
     * Batch insert attendance records - more efficient than saveAll for large datasets
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO daily_attendance_details 
            (employee_id, cycle_id, attendance_date, status, marked_by, marked_at)
            VALUES (:employeeId, :cycleId, :attendanceDate, :status, :markedBy, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int batchInsertAttendance(
            @Param("employeeId") String employeeId,
            @Param("cycleId") Long cycleId,
            @Param("attendanceDate") LocalDate attendanceDate,
            @Param("status") String status,
            @Param("markedBy") String markedBy);

    // ─── Bulk status update ───────────────────────────────────────────────────

    @Modifying
    @Transactional
    @Query("""
            UPDATE DailyAttendanceDetail d
               SET d.status    = :status,
                   d.remarks   = :remarks,
                   d.markedBy  = :markedBy,
                   d.markedAt  = CURRENT_TIMESTAMP
             WHERE d.employeeId     = :employeeId
               AND d.attendanceDate = :attendanceDate
            """)
    int updateAttendanceStatus(
            @Param("employeeId") String employeeId,
            @Param("attendanceDate") LocalDate attendanceDate,
            @Param("status") String status,
            @Param("remarks") String remarks,
            @Param("markedBy") String markedBy);

    /**
     * Bulk update status for multiple employees on same date
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE DailyAttendanceDetail d
               SET d.status = :status,
                   d.markedBy = :markedBy,
                   d.markedAt = CURRENT_TIMESTAMP
             WHERE d.attendanceDate = :attendanceDate
               AND d.employeeId IN :employeeIds
            """)
    int bulkUpdateAttendanceStatus(
            @Param("attendanceDate") LocalDate attendanceDate,
            @Param("employeeIds") List<String> employeeIds,
            @Param("status") DailyAttendanceDetail.AttendanceStatus status,
            @Param("markedBy") String markedBy);

    // ─── Aggregate / existence queries ───────────────────────────────────────

    long countByAttendanceDateAndStatus(
            LocalDate date, DailyAttendanceDetail.AttendanceStatus status);

    @Query("""
            SELECT COUNT(DISTINCT d.employeeId)
            FROM   DailyAttendanceDetail d
            WHERE  d.attendanceDate = :date
            """)
    long countDistinctEmployeesByAttendanceDate(@Param("date") LocalDate date);

    /**
     * Check if attendance exists for specific employee and date range
     */
    @Query("""
            SELECT COUNT(d) > 0
            FROM DailyAttendanceDetail d
            WHERE d.employeeId = :employeeId
            AND d.attendanceDate BETWEEN :startDate AND :endDate
            """)
    boolean existsByEmployeeIdAndDateRange(
            @Param("employeeId") String employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get attendance for date range with statuses - for reports
     */
    @Query("""
            SELECT d.attendanceDate, d.status, COUNT(d)
            FROM DailyAttendanceDetail d
            WHERE d.attendanceCycle.cycleId = :cycleId
            GROUP BY d.attendanceDate, d.status
            ORDER BY d.attendanceDate
            """)
    List<Object[]> getDailyStatusCounts(@Param("cycleId") Long cycleId);
}