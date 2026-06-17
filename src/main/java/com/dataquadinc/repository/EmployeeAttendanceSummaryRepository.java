package com.dataquadinc.repository;

import com.dataquadinc.model.EmployeeAttendanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeAttendanceSummaryRepository extends JpaRepository<EmployeeAttendanceSummary, Long> {

    Optional<EmployeeAttendanceSummary> findByEmployeeIdAndAttendanceCycle_CycleId(String employeeId, Long cycleId);

    @Modifying
    @Transactional
    @Query("UPDATE EmployeeAttendanceSummary e SET e.totalPayDays = :totalPayDays WHERE e.employeeAttendanceId = :summaryId")
    int updateTotalPayDays(@Param("summaryId") Long summaryId, @Param("totalPayDays") Integer totalPayDays);

    @Query("SELECT s FROM EmployeeAttendanceSummary s WHERE s.attendanceCycle.cycleId = :cycleId")
    List<EmployeeAttendanceSummary> findByAttendanceCycle_CycleId(@Param("cycleId") Long cycleId);

    @Query("SELECT s FROM EmployeeAttendanceSummary s LEFT JOIN FETCH s.attendanceCycle WHERE s.attendanceCycle.cycleId = :cycleId")
    List<EmployeeAttendanceSummary> findByAttendanceCycle_CycleIdWithDetails(@Param("cycleId") Long cycleId);

    // ============ NEW OPTIMIZED QUERIES ============

    /**
     * Find summaries for specific employees in a cycle - optimized for paginated bulk requests
     */
    @Query("""
            SELECT s FROM EmployeeAttendanceSummary s 
            WHERE s.attendanceCycle.cycleId = :cycleId 
            AND s.employeeId IN :employeeIds
            """)
    List<EmployeeAttendanceSummary> findByAttendanceCycle_CycleIdAndEmployeeIdIn(
            @Param("cycleId") Long cycleId,
            @Param("employeeIds") List<String> employeeIds);

    /**
     * Get summary count for a cycle (lightweight)
     */
    @Query("SELECT COUNT(s) FROM EmployeeAttendanceSummary s WHERE s.attendanceCycle.cycleId = :cycleId")
    long countByAttendanceCycle_CycleId(@Param("cycleId") Long cycleId);

    /**
     * Get aggregate statistics for a cycle in one query
     */
    @Query("""
            SELECT 
                SUM(s.totalWorkedDays) as totalWorkedDays,
                SUM(s.totalLeavesTaken) as totalLeavesTaken,
                SUM(s.casualLeaves) as totalCasualLeaves,
                SUM(s.sickLeaves) as totalSickLeaves,
                SUM(s.lossOfPayLeaves) as totalLOPLeaves,
                SUM(s.specialLeaves) as totalSpecialLeaves,
                AVG(s.totalWorkedDays * 100.0 / s.totalWorkingDays) as avgAttendance
            FROM EmployeeAttendanceSummary s 
            WHERE s.attendanceCycle.cycleId = :cycleId
            """)
    List<Object[]> getCycleAggregateStatistics(@Param("cycleId") Long cycleId);

    /**
     * Batch delete summaries for a cycle (used when regenerating)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM EmployeeAttendanceSummary s WHERE s.attendanceCycle.cycleId = :cycleId")
    void deleteByAttendanceCycle_CycleId(@Param("cycleId") Long cycleId);

    /**
     * Get employees with low attendance (for reporting)
     */
    @Query("""
            SELECT s.employeeId, s.totalWorkedDays, s.totalWorkingDays 
            FROM EmployeeAttendanceSummary s 
            WHERE s.attendanceCycle.cycleId = :cycleId 
            AND s.totalWorkingDays > 0 
            AND (s.totalWorkedDays * 100.0 / s.totalWorkingDays) < :threshold
            ORDER BY (s.totalWorkedDays * 100.0 / s.totalWorkingDays) ASC
            """)
    List<Object[]> findLowAttendanceEmployees(
            @Param("cycleId") Long cycleId,
            @Param("threshold") double threshold);

    /**
     * Find summary by employee ID with cycle details - optimized for single employee view
     */
    @Query("""
            SELECT s FROM EmployeeAttendanceSummary s 
            LEFT JOIN FETCH s.attendanceCycle 
            WHERE s.employeeId = :employeeId 
            AND s.attendanceCycle.cycleId = :cycleId
            """)
    Optional<EmployeeAttendanceSummary> findByEmployeeIdAndCycleIdWithDetails(
            @Param("employeeId") String employeeId,
            @Param("cycleId") Long cycleId);

    /**
     * Get all summaries for a cycle with employee details - for export
     */
    @Query(value = """
            SELECT s.*, u.user_name as employee_name, u.designation, u.department 
            FROM employee_attendance_summary s
            JOIN user_details u ON s.employee_id = u.user_id
            WHERE s.cycle_id = :cycleId
            ORDER BY u.user_name
            """, nativeQuery = true)
    List<Object[]> findCycleSummariesWithEmployeeDetails(@Param("cycleId") Long cycleId);


}