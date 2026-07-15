package com.dataquadinc.repository;

import com.dataquadinc.model.AttendanceMonthConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceMonthConfigRepository
        extends JpaRepository<AttendanceMonthConfig, Long> {

    Optional<AttendanceMonthConfig>
    findByAttendanceMonthAndAttendanceYearAndEntity(
            Integer attendanceMonth,
            Integer attendanceYear,
            String entity
    );
}
