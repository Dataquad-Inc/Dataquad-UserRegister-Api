package com.dataquadinc.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_attendance_summary")
@Data
public class EmployeeAttendanceSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_attendance_id")
    private Long employeeAttendanceId;

    @Column(name = "employee_id", nullable = false, length = 50)
    private String employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private AttendanceCycle attendanceCycle;

    @Column(name = "total_working_days")
    private Integer totalWorkingDays;

    @Column(name = "total_week_offs")
    private Integer totalWeekOffs;

    @Column(name = "total_public_holidays")
    private Integer totalPublicHolidays;

    @Column(name = "casual_leaves")
    private Integer casualLeaves = 0;

    @Column(name = "sick_leaves")
    private Integer sickLeaves = 0;

    @Column(name = "loss_of_pay_leaves")
    private Integer lossOfPayLeaves = 0;

    @Column(name = "special_leaves")
    private Integer specialLeaves = 0;

    @Column(name = "total_worked_days")
    private Integer totalWorkedDays = 0;

    @Column(name = "total_leaves_taken")
    private Integer totalLeavesTaken = 0;

    @Column(name = "total_pay_days")
    private Integer totalPayDays = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}