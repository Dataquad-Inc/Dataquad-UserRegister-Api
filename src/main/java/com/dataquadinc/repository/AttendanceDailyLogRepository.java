package com.dataquadinc.repository;

import com.dataquadinc.model.AttendanceDailyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceDailyLogRepository
        extends JpaRepository<AttendanceDailyLog, Long> {

    List<AttendanceDailyLog> findAllByOrderByAttendanceDateDesc();
}