package com.dataquadinc.exceptions;

import com.dataquadinc.dto.ErrorResponseBean;
import com.dataquadinc.dto.LoginResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Helper method to build ErrorResponseBean
    private <T> ResponseEntity<ErrorResponseBean<T>> buildErrorResponse(boolean success, String message, T data, Map<String, String> errorDetails, HttpStatus status) {
        // Using Lombok's builder method directly
        ErrorResponseBean<T> errorResponse = new  ErrorResponseBean.Builder<T>()
                .success(success)
                .message(message)
                .data(data)
                .error(errorDetails)
                .build();

        // Return the ResponseEntity with appropriate status
        return new ResponseEntity<>(errorResponse, status);
    }

    // Handle UserInactiveException (returns 403 Forbidden)
    @ExceptionHandler(UserInactiveException.class)
    public ResponseEntity<LoginResponseDTO> handleUserInactiveException(UserInactiveException e) {
        // Create error response for inactive user login attempt
        LoginResponseDTO errorResponse = new LoginResponseDTO(
                false,
                "Unsuccessful",
                null,
                new LoginResponseDTO.ErrorDetails("403", e.getMessage()) // 403 for forbidden action
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse); // Return 403 Forbidden status
    }

    // Handle custom ValidationException
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseBean<Map<String, String>>> handleValidationException(ValidationException ex) {
        // Combining errors from ValidationException into a single message
        String errorMessage = String.join(", ", ex.getErrors().values());

        // Build error details with error code and message
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("errorcode", "300");
        errorDetails.put("errormessage", errorMessage);

        // Return a response with a BAD_REQUEST status
        return buildErrorResponse(false, "Validation failed", null, errorDetails, HttpStatus.OK);
    }

    // Handle MethodArgumentNotValidException
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseBean<Map<String, String>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // Collecting all field validation errors
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = "Invalid value for field: " + fieldName + ". Please check and provide the correct information.";
            errors.put(fieldName, message);
        });

        // Returning a BAD_REQUEST status with the validation errors
        return buildErrorResponse(false, "Validation error", errors, null, HttpStatus.OK);
    }

    // Handle custom InvalidUserException
    @ExceptionHandler(InvalidUserException.class)
    public ResponseEntity<ErrorResponseBean<Map<String, String>>> handleInvalidUserException(InvalidUserException ex) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("userId", ex.getMessage()); // Accessing the message from InvalidUserException

        return buildErrorResponse(false, "User not found", null, errorDetails, HttpStatus.NOT_FOUND);
    }

    // Handle Invalid Credentials Exception (returns 401 Unauthorized)
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

    // Handle User Already Logged In Exception (returns 400 Bad Request)
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

    // Handle User Not Found Exception (returns 404 Not Found)
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

    // Handle any other exceptions (return 500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<LoginResponseDTO> handleGeneralException(Exception e) {
        LoginResponseDTO errorResponse = new LoginResponseDTO(
                false,
                "Unsuccessful",
                null,
                new LoginResponseDTO.ErrorDetails("500", "An unexpected error occurred: " + e.getMessage())
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    @ExceptionHandler(NoSuchUserException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchUserException(NoSuchUserException e) {
        ErrorDto error=new ErrorDto(404,e.getMessage());
        ErrorResponse errorResponse=new ErrorResponse(
                false,"User Not Found",new ArrayList<>(),error);

        return new ResponseEntity<>(errorResponse,HttpStatus.NOT_FOUND);
    }

}
