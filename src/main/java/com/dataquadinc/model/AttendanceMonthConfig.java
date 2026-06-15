package com.dataquadinc.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "attendance_month_config")
@Data
public class AttendanceMonthConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer attendanceMonth;

    private Integer attendanceYear;

    /*
    Example:
    June Attendance

    fromDate = May 26
    toDate   = June 25
     */
    private LocalDate fromDate;

    private LocalDate toDate;

    /*
    Public holidays
    Stored as JSON
     */
    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    private List<LocalDate> publicHolidays = new ArrayList<>();

    private Boolean isLocked = false;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
