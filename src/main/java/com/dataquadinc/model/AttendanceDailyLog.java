package com.dataquadinc.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "attendance_daily_log")
@Data
public class AttendanceDailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    private String department;

    @Column(name = "attendance_date")
    private LocalDate attendanceDate;

    @Type(JsonType.class)
    @Column(name = "raw_logs", columnDefinition = "json")
    private List<String> rawLogs = new ArrayList<>();

    @Column(name = "login_time")
    private String loginTime;

    @Column(name = "logout_time")
    private String logoutTime;

    @Column(name = "uploaded_file_name")
    private String uploadedFileName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;


    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public LocalDate getAttendanceDate() {return attendanceDate;}

    public void setAttendanceDate(LocalDate attendanceDate) {this.attendanceDate = attendanceDate;}
}
