package com.dataquadinc.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CycleResponseDTO {
    private Long cycleId;
    private String attendanceMonth;
    private Integer attendanceYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDaysInCycle;
    private Integer totalWorkingDays;
    private Integer totalWeekOffs;
    private Integer totalPublicHolidays;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}