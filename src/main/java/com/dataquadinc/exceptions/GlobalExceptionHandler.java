package com.dataquadinc.exceptions;

import com.dataquadinc.dto.ErrorResponseBean;
import com.dataquadinc.dto.LoginResponseDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Helper method to build ErrorResponseBean
    private <T> ResponseEntity<ErrorResponseBean<T>> buildErrorResponse(boolean success, String message, T data, Map<String, String> errorDetails, HttpStatus status) {
        ErrorResponseBean<T> errorResponse = new ErrorResponseBean.Builder<T>()
                .success(success)
                .message(message)
                .data(data)
                .error(errorDetails)
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }

    // ============ EXISTING EXCEPTION HANDLERS ============

    // Handle UserInactiveException (returns 403 Forbidden)
    @ExceptionHandler(UserInactiveException.class)
    public ResponseEntity<LoginResponseDTO> handleUserInactiveException(UserInactiveException e) {
        LoginResponseDTO errorResponse = new LoginResponseDTO(
                false,
                "Unsuccessful",
                null,
                new LoginResponseDTO.ErrorDetails("403", e.getMessage())
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // Handle custom ValidationException
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseBean<Map<String, String>>> handleValidationException(ValidationException ex) {
        String errorMessage = String.join(", ", ex.getErrors().values());
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("errorcode", "300");
        errorDetails.put("errormessage", errorMessage);
        return buildErrorResponse(false, "Validation failed", null, errorDetails, HttpStatus.OK);
    }

    // Handle MethodArgumentNotValidException
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseBean<Map<String, String>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = "Invalid value for field: " + fieldName + ". Please check and provide the correct information.";
            errors.put(fieldName, message);
        });
        return buildErrorResponse(false, "Validation error", errors, null, HttpStatus.OK);
    }

    // Handle custom InvalidUserException
    @ExceptionHandler(InvalidUserException.class)
    public ResponseEntity<ErrorResponseBean<Map<String, String>>> handleInvalidUserException(InvalidUserException ex) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("userId", ex.getMessage());
        return buildErrorResponse(false, "User not found", null, errorDetails, HttpStatus.NOT_FOUND);
    }

    // Handle Invalid Credentials Exception
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<LoginResponseDTO> handleInvalidCredentials(InvalidCredentialsException e) {
        LoginResponseDTO errorResponse = new LoginResponseDTO(
                false,
                "Unsuccessful",
                null,
                new LoginResponseDTO.ErrorDetails("300", e.getMessage())
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // Handle User Already Logged In Exception
    @ExceptionHandler(UserAlreadyLoggedInException.class)
    public ResponseEntity<LoginResponseDTO> handleUserAlreadyLoggedIn(UserAlreadyLoggedInException e) {
        LoginResponseDTO errorResponse = new LoginResponseDTO(
                false,
                "Unsuccessful",
                null,
                new LoginResponseDTO.ErrorDetails("201", e.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(errorResponse);
    }

    // Handle User Not Found Exception
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<LoginResponseDTO> handleUserNotFound(UserNotFoundException e) {
        LoginResponseDTO errorResponse = new LoginResponseDTO(
                false,
                "Unsuccessful",
                null,
                new LoginResponseDTO.ErrorDetails("404", e.getMessage())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(NoSuchUserException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchUserException(NoSuchUserException e) {
        ErrorDto error = new ErrorDto(String.valueOf(HttpStatus.NOT_FOUND), e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "User Not Found",
                new ArrayList<>(),
                error
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // ============ ATTENDANCE EXCEPTION HANDLERS ============

    // Handle AttendanceException
    @ExceptionHandler(AttendanceException.class)
    public ResponseEntity<ErrorResponse> handleAttendanceException(AttendanceException ex) {
        log.error("Attendance exception: {}", ex.getMessage());
        ErrorDto error = new ErrorDto("ATTENDANCE_001", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                ex.getMessage(),
                new ArrayList<>(),
                error
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // Handle CycleNotFoundException
    @ExceptionHandler(CycleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCycleNotFoundException(CycleNotFoundException ex) {
        log.error("Cycle not found: {}", ex.getMessage());
        ErrorDto error = new ErrorDto("CYCLE_001", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Attendance cycle not found",
                new ArrayList<>(),
                error
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // Handle EmployeeNotFoundException
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFoundException(EmployeeNotFoundException ex) {
        log.error("Employee not found: {}", ex.getMessage());
        ErrorDto error = new ErrorDto("EMPLOYEE_001", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Employee not found",
                new ArrayList<>(),
                error
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // Handle AttendanceRecordNotFoundException
    @ExceptionHandler(AttendanceRecordNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAttendanceRecordNotFoundException(AttendanceRecordNotFoundException ex) {
        log.error("Attendance record not found: {}", ex.getMessage());
        ErrorDto error = new ErrorDto("ATTENDANCE_002", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Attendance record not found",
                new ArrayList<>(),
                error
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // Handle FutureDateAttendanceException
    @ExceptionHandler(FutureDateAttendanceException.class)
    public ResponseEntity<ErrorResponse> handleFutureDateAttendanceException(FutureDateAttendanceException ex) {
        log.error("Future date attendance: {}", ex.getMessage());
        ErrorDto error = new ErrorDto("ATTENDANCE_003", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Cannot mark attendance for future date",
                new ArrayList<>(),
                error
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // Handle BulkAttendanceException
    @ExceptionHandler(BulkAttendanceException.class)
    public ResponseEntity<ErrorResponse> handleBulkAttendanceException(BulkAttendanceException ex) {
        log.error("Bulk attendance exception: {}", ex.getMessage());
        ErrorDto error = new ErrorDto("BULK_001", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Bulk attendance operation failed",
                new ArrayList<>(),
                error
        );
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(errorResponse);
    }

    // Handle ConstraintViolationException
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (v1, v2) -> v1 + ", " + v2
                ));
        log.error("Constraint violation: {}", errors);
        ErrorDto error = new ErrorDto("VALIDATION_002", "Constraint validation failed");
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Validation error",
                new ArrayList<>(errors.values()),
                error
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    // Handle any other exceptions (fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ErrorDto error = new ErrorDto("SYSTEM_001", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "An unexpected error occurred",
                new ArrayList<>(),
                error
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }


    // Add to GlobalExceptionHandler.java

    @ExceptionHandler(ProbationLeaveException.class)
    public ResponseEntity<ErrorResponse> handleProbationLeaveException(ProbationLeaveException ex) {
        log.error("Probation leave exception: {}", ex.getMessage());
        ErrorDto error = new ErrorDto("LEAVE_001", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Probation leave restriction",
                new ArrayList<>(),
                error
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(SandwichLeaveException.class)
    public ResponseEntity<ErrorResponse> handleSandwichLeaveException(SandwichLeaveException ex) {
        log.error("Sandwich leave exception: {}", ex.getMessage());
        ErrorDto error = new ErrorDto("LEAVE_002", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Sandwich leave policy violation",
                new ArrayList<>(),
                error
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(LeaveQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleLeaveQuotaExceededException(LeaveQuotaExceededException ex) {
        log.error("Leave quota exceeded: {}", ex.getMessage());
        ErrorDto error = new ErrorDto("LEAVE_003", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Leave quota exceeded",
                new ArrayList<>(),
                error
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}