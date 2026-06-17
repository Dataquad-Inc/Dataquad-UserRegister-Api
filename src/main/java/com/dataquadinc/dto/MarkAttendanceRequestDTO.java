package com.dataquadinc.dto;

import com.dataquadinc.model.DailyAttendanceDetail;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class MarkAttendanceRequestDTO {
    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    @NotNull(message = "Status is required")
    private DailyAttendanceDetail.AttendanceStatus status;

    private String remarks;
}