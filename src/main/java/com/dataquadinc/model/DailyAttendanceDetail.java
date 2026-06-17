// 1. Updated DailyAttendanceDetail model - REMOVED EL, A, OD
package com.dataquadinc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_attendance_details",
        indexes = {
                @Index(name = "idx_employee_attendance_date",
                        columnList = "employee_id, attendance_date")
        })
@Data
public class DailyAttendanceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_detail_id")
    private Long attendanceDetailId;

    @Column(name = "employee_id", nullable = false, length = 50)
    private String employeeId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private AttendanceCycle attendanceCycle;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false, length = 3)
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "marked_by", length = 50)
    private String markedBy;

    @CreationTimestamp
    @Column(name = "marked_at")
    private LocalDateTime markedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum AttendanceStatus {
        P("Present"),
        WO("Week Off"),
        PH("Public Holiday"),
        CL("Casual Leave"),
        SL("Sick Leave"),
        LOP("Loss Of Pay"),
        HD("Half Day"),
        WFH("Work From Home"),
        SP("Special Leave");  // Special Leave (paid)

        private final String description;

        AttendanceStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}