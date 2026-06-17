package com.dataquadinc.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BulkAttendanceRequestDTO {
    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;
}
