package com.dataquadinc.repository;

import com.dataquadinc.model.AttendanceMonthConfig;
import com.dataquadinc.model.EmployeeAttendance;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<EmployeeAttendance, Long> {

    /*
     Check attendance already exists for employee + date
     */
    Optional<EmployeeAttendance> findByEmployeeIdAndAttendanceDate(
            String employeeId,
            LocalDate attendanceDate
    );

    /*
     Dashboard month data
     */
    @Query("""
            SELECT ea
            FROM EmployeeAttendance ea
            WHERE ea.attendanceMonth = :month
            AND ea.attendanceYear = :year
            ORDER BY ea.employeeId, ea.attendanceDate
            """)
    List<EmployeeAttendance> getAttendanceByMonthAndYear(
            @Param("month") Integer month,
            @Param("year") Integer year
    );

    /*
     Employee month attendance
     */
    @Query("""
            SELECT ea
            FROM EmployeeAttendance ea
            WHERE ea.employeeId = :employeeId
            AND ea.attendanceMonth = :month
            AND ea.attendanceYear = :year
            ORDER BY ea.attendanceDate
            """)
    List<EmployeeAttendance>
    getEmployeeAttendanceMonth(
            @Param("employeeId") String employeeId,
            @Param("month") Integer month,
            @Param("year") Integer year
    );

    /*
     Week submission
     */
    @Query("""
            SELECT ea
            FROM EmployeeAttendance ea
            WHERE ea.attendanceMonth = :month
            AND ea.attendanceYear = :year
            AND ea.weekNumber = :weekNumber
            """)
    List<EmployeeAttendance> getWeekAttendance(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("weekNumber") Integer weekNumber
    );

    /*
     Lock check
     */
    @Query("""
            SELECT COUNT(ea)
            FROM EmployeeAttendance ea
            WHERE ea.attendanceMonth = :month
            AND ea.attendanceYear = :year
            AND ea.weekNumber = :weekNumber
            AND ea.approvalStatus = 'APPROVED'
            """)
    Long countApprovedWeek(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("weekNumber") Integer weekNumber
    );

    /*
     Find attendance for specific date
     */
    List<EmployeeAttendance> findByAttendanceDate(LocalDate attendanceDate);

    /*
     Public Holiday check
     */
    @Query("""
            SELECT COUNT(ea)
            FROM EmployeeAttendance ea
            WHERE ea.attendanceDate = :date
            AND ea.isPublicHoliday = true
            """)
    Long countPublicHoliday(
            @Param("date") LocalDate date
    );

    /*
     Attendance range
     */
    @Query("""
            SELECT ea
            FROM EmployeeAttendance ea
            WHERE ea.employeeId = :employeeId
            AND ea.attendanceDate BETWEEN :fromDate AND :toDate
            ORDER BY ea.attendanceDate
            """)
    List<EmployeeAttendance> findAttendanceBetweenDates(
            @Param("employeeId") String employeeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /*
     Sandwich leave calculation
     */
    @Query("""
            SELECT ea
            FROM EmployeeAttendance ea
            WHERE ea.employeeId = :employeeId
            AND ea.attendanceDate IN :dates
            """)
    List<EmployeeAttendance> findAttendanceForDates(
            @Param("employeeId") String employeeId,
            @Param("dates") List<LocalDate> dates
    );

    /*
     Check if week already submitted
     */
    @Query("""
            SELECT COUNT(ea)
            FROM EmployeeAttendance ea
            WHERE ea.attendanceMonth = :month
            AND ea.attendanceYear = :year
            AND ea.weekNumber = :weekNumber
            AND ea.approvalStatus = 'SUBMITTED'
            """)
    Long countSubmittedWeek(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("weekNumber") Integer weekNumber
    );

    /*
     Get employee attendance date
     */
    @Query("""
            SELECT ea
            FROM EmployeeAttendance ea
            WHERE ea.employeeId = :employeeId
            AND ea.attendanceDate = :attendanceDate
            """)
    Optional<EmployeeAttendance> getEmployeeAttendanceByDate(
            @Param("employeeId") String employeeId,
            @Param("attendanceDate") LocalDate attendanceDate
    );

    @Query("""
            SELECT ea
            FROM EmployeeAttendance ea
            WHERE ea.attendanceMonth = :month
            AND ea.attendanceYear = :year
            AND ea.weekNumber = :weekNumber
            AND ea.monthConfig.entity = :entity
            """)
    List<EmployeeAttendance>
    findByMonthYearAndWeek(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("weekNumber") Integer weekNumber,
            @Param("entity")String entity
    );


    /*
     Dashboard Data
     */

    @Query("""
            SELECT ea
            FROM EmployeeAttendance ea
            WHERE ea.attendanceMonth = :month
            AND ea.attendanceYear = :year
            ORDER BY ea.employeeId,
                     ea.attendanceDate
            """)
    List<EmployeeAttendance>
    findAttendanceByMonthAndYear(
            @Param("month") Integer month,
            @Param("year") Integer year
    );

    /*
     Employee Monthly Attendance
     */



    /*
     Week Submit / Approve
     */


    /*
     Edit Month Setup

     Delete only WO / PH
     Keep HR entries:
     P,L,WH,HD,LL
     */
    @Modifying
    @Transactional
    @Query("""
            DELETE
            FROM EmployeeAttendance ea
            WHERE ea.attendanceMonth = :month
            AND ea.attendanceYear = :year
            AND (
                ea.attendanceStatus = 'WO'
                OR
                ea.attendanceStatus = 'PH'
            )
            """)
    void deleteAutoGeneratedAttendance(
            @Param("month") Integer month,
            @Param("year") Integer year
    );

    boolean existsByEmployeeIdAndAttendanceDate(
            String employeeId,
            LocalDate attendanceDate
    );
    @Transactional
    @Modifying
    @Query("""
DELETE FROM EmployeeAttendance ea
WHERE ea.attendanceMonth = :month
AND ea.attendanceYear = :year
AND ea.monthConfig.entity = :entity
""")
    void deleteAttendanceByMonth(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("entity") String entity
    );

    @Modifying
    @Transactional
    @Query("""
       DELETE FROM EmployeeAttendance ea
       WHERE ea.monthConfig = :monthConfig
       AND ea.monthConfig.entity=:entity
       """)
    void deleteByMonthConfig(
            @Param("monthConfig") AttendanceMonthConfig monthConfig
    );

    @Query("""
       SELECT ea
       FROM EmployeeAttendance ea
       WHERE ea.attendanceMonth=:month
       AND ea.attendanceYear=:year
       AND ea.monthConfig.entity = :entity
       ORDER BY ea.employeeId,
                ea.attendanceDate
       """)
    List<EmployeeAttendance> findMonthAttendance(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("entity")String entity);

    @Query("""
       SELECT ea
       FROM EmployeeAttendance ea
       WHERE ea.attendanceMonth=:month
       AND ea.attendanceYear=:year
       AND ea.monthConfig.entity = :entity
       AND ea.approvalStatus='SUBMITTED'
       ORDER BY ea.employeeId,
                ea.attendanceDate
       """)
    List<EmployeeAttendance> findSubmittedMonthAttendance(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("entity") String entity);

    @Query("""
       SELECT ea
       FROM EmployeeAttendance ea
       WHERE ea.attendanceMonth=:month
       AND ea.attendanceYear=:year
       AND ea.weekNumber=:weekNumber
       AND ea.monthConfig.entity = :entity
       AND ea.approvalStatus='SUBMITTED'
       ORDER BY ea.employeeId,
                ea.attendanceDate
       """)
    List<EmployeeAttendance> findSubmittedWeekAttendance(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("weekNumber") Integer weekNumber,
            @Param("entity") String entity);

    @Query("""
       SELECT COUNT(ea)
       FROM EmployeeAttendance ea
       WHERE ea.attendanceMonth=:month
       AND ea.attendanceYear=:year
       AND ea.monthConfig.entity = :entity
       AND ea.approvalStatus='APPROVED'
       """)
    Long countApprovedMonth(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("entity") String Entity);

    @Query("""
       SELECT COUNT(ea)
       FROM EmployeeAttendance ea
       WHERE ea.attendanceMonth=:month
       AND ea.attendanceYear=:year
       AND ea.monthConfig.entity = :entity
       AND ea.approvalStatus='SUBMITTED'
       """)
    Long countSubmittedMonth(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("entity") String entity);



    @Transactional
    @Modifying
    @Query("""
            DELETE FROM EmployeeAttendance ea
            WHERE ea.attendanceMonth = :month
            AND ea.attendanceYear = :year
            AND ea.monthConfig.entity = :entity
            """)
    int deleteAttendanceByMonthconfig(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("entity") String entity
    );

    @Query("""
SELECT COUNT(ea)
FROM EmployeeAttendance ea
WHERE ea.attendanceMonth = :month
AND ea.attendanceYear = :year
AND ea.monthConfig.entity = :entity
AND (
      ea.approvalStatus='SUBMITTED'
      OR
      ea.approvalStatus='APPROVED'
)
""")
    Long countSubmittedOrApprovedAttendance(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("entity") String entity);

    @Query("""
       SELECT ea
       FROM EmployeeAttendance ea
       WHERE ea.employeeId = :employeeId
       AND ea.attendanceMonth = :month
       AND ea.attendanceYear = :year
       AND ea.approvalStatus = 'APPROVED'
       ORDER BY ea.attendanceDate
       """)
    List<EmployeeAttendance> getApprovedEmployeeAttendanceMonth(
            @Param("employeeId") String employeeId,
            @Param("month") Integer month,
            @Param("year") Integer year
    );
}