package com.dataquadinc.dto;

import com.dataquadinc.model.UserType;  // Correct import
import lombok.Data;

import java.time.LocalDateTime;

@Data

public class LoginResponseDTO {
    private boolean success;
    private String message;
    private Payload payload;
    private ErrorDetails error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public ErrorDetails getError() {
        return error;
    }

    public void setError(ErrorDetails error) {
        this.error = error;
    }

    @Data

    public static class Payload {
        private String userId;
        private String userName;
        private String email;
        private UserType roleType;  // Usage of UserType
        private LocalDateTime loginTimestamp;
        private String encryptionKey;
        private String entity;

        // Add a constructor to accept these parameters


        public Payload(String userId, String userName,
                       String email, UserType roleType,
                       LocalDateTime loginTimestamp,
                       String encryptionKey, String entity) {
            this.userId = userId;
            this.userName = userName;
            this.email = email;
            this.roleType = roleType;
            this.loginTimestamp = loginTimestamp;
            this.encryptionKey = encryptionKey;
            this.entity = entity;
        }
    }
    @Data

    public static class ErrorDetails {
        private String errorCode;
        private String errorMessage;

        public ErrorDetails(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }

    public LoginResponseDTO(boolean success, String message, Payload payload) {
        this.success = success;
        this.message = message;
        this.payload = payload;
        this.error = null;
    }

    public LoginResponseDTO(boolean success, String message, Payload payload, ErrorDetails error) {
        this.success = success;
        this.message = message;
        this.payload = payload;
        this.error = error;
    }
}
