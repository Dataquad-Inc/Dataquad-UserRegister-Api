package com.dataquadinc.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AttendanceCycleDTO {
    private Long cycleId;

    @NotBlank(message = "Attendance month is required")
    private String attendanceMonth;

    @NotNull(message = "Attendance year is required")
    @Min(value = 2020, message = "Year must be >= 2020")
    @Max(value = 2100, message = "Year must be <= 2100")
    private Integer attendanceYear;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Integer totalDaysInCycle;
    private Integer totalWorkingDays;
    private Integer totalWeekOffs;
    private Integer totalPublicHolidays;
    private String status;
}
