package com.dataquadinc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "attendance_cycles")
@Data
public class AttendanceCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cycle_id")
    private Long cycleId;

    @Column(name = "attendance_month", nullable = false, length = 20)
    private String attendanceMonth;

    @Column(name = "attendance_year", nullable = false)
    private Integer attendanceYear;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days_in_cycle", nullable = false)
    private Integer totalDaysInCycle;

    @Column(name = "total_working_days")
    private Integer totalWorkingDays;

    @Column(name = "total_week_offs")
    private Integer totalWeekOffs;

    @Column(name = "total_public_holidays")
    private Integer totalPublicHolidays;

    @Column(length = 10)
    private String status = "OPEN";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @JsonIgnore  // Add this to prevent circular reference
    @OneToMany(mappedBy = "attendanceCycle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EmployeeAttendanceSummary> attendanceSummaries = new ArrayList<>();

    @JsonIgnore  // Add this to prevent circular reference
    @OneToMany(mappedBy = "attendanceCycle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DailyAttendanceDetail> attendanceDetails = new ArrayList<>();
}