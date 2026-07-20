package com.dataquadinc.repository;

import com.dataquadinc.model.AttendanceMonthConfig;
import feign.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AttendanceMonthConfigRepository
        extends JpaRepository<AttendanceMonthConfig, Long> {

    Optional<AttendanceMonthConfig>
    findByAttendanceMonthAndAttendanceYearAndEntity(
            Integer attendanceMonth,
            Integer attendanceYear,
            String entity
    );

    @Transactional
    @Modifying
    @Query("""
            DELETE FROM AttendanceMonthConfig c
            WHERE c.attendanceMonth = :month
            AND c.attendanceYear = :year
            AND c.entity = :entity
            """)
    int deleteConfig(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("entity") String entity
    );
}
