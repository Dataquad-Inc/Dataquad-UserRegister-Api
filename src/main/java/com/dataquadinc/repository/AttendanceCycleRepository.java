package com.dataquadinc.repository;

import com.dataquadinc.model.AttendanceCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceCycleRepository extends JpaRepository<AttendanceCycle, Long> {

    // ─── Basic finders ────────────────────────────────────────────────────────

    Optional<AttendanceCycle> findByAttendanceYearAndAttendanceMonth(
            Integer year, String month);

    /**
     * Find the cycle whose [startDate, endDate] range contains the given date.
     */
    @Query("""
            SELECT c FROM AttendanceCycle c
            WHERE :date BETWEEN c.startDate AND c.endDate
            """)
    Optional<AttendanceCycle> findByDate(@Param("date") LocalDate date);

    /**
     * Returns the most recently started OPEN cycle.
     */
    @Query("""
            SELECT c FROM AttendanceCycle c
            WHERE c.status = 'OPEN'
            ORDER BY c.startDate DESC
            """)
    Optional<AttendanceCycle> findLatestOpenCycle();

    // ─── OPTIMISED: sorted list – avoids client-side sorting ─────────────────

    /**
     * Returns all cycles ordered newest-first.  The service's getAllCycles()
     * uses this instead of findAll() so the result is already sorted.
     */
    @Query("""
            SELECT c FROM AttendanceCycle c
            ORDER BY c.attendanceYear DESC, c.startDate DESC
            """)
    List<AttendanceCycle> findAllByOrderByAttendanceYearDescAttendanceMonthDesc();

    // ─── Overlap check (avoids loading all cycles into memory) ───────────────

    /**
     * Returns {@code true} when ANY open cycle overlaps with [start, end].
     * Used in createCycle() instead of the old findAll() + stream filter.
     *
     * Two ranges [s1,e1] and [s2,e2] do NOT overlap iff e1 < s2 OR e2 < s1.
     * They DO overlap otherwise, i.e.:  NOT (e1 < s2 OR e2 < s1).
     */
    @Query("""
            SELECT COUNT(c) > 0
            FROM   AttendanceCycle c
            WHERE  c.status = 'OPEN'
              AND  c.startDate <= :endDate
              AND  c.endDate   >= :startDate
            """)
    boolean existsOpenCycleOverlapping(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}