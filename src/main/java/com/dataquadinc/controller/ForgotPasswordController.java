package com.dataquadinc.controller;

import com.dataquadinc.dto.ForgotPasswordDto;
import com.dataquadinc.dto.ForgotResponseDto;
import com.dataquadinc.service.ForgotPasswordService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com","http://localhost:3000","http://192.168.0.135:8080","http://192.168.0.135",
        "http://154.210.288.26",
        "http://192.168.0.203:3000",
        "http://192.168.0.167:3000"})

@RestController
@RequestMapping("/users")
public class ForgotPasswordController {

    @Autowired
    private ForgotPasswordService forgotPasswordService;

    // Forgot Password (Generate OTP)
    @PostMapping("/send-otp")
    public ResponseEntity<ForgotResponseDto> forgotPassword(@RequestBody ForgotPasswordDto forgotPasswordDto) {
        String email = forgotPasswordDto.getEmail();
        System.out.println("Received email: " + email);

        // Validate email
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(new ForgotResponseDto(false, "Email cannot be null or empty", "Invalid email"));
        }

        // Generate OTP
        try {
            ForgotResponseDto response = forgotPasswordService.generateOtp(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ForgotResponseDto(false, "Error generating OTP", e.getMessage()));
        }
    }

    // Verify OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<ForgotResponseDto> verifyOtp(@RequestBody ForgotPasswordDto forgotPasswordDto) {
        String email = forgotPasswordDto.getEmail();
        String otp = forgotPasswordDto.getOtp();

        if (email == null || otp == null || email.isEmpty() || otp.isEmpty()) {
            return ResponseEntity.badRequest().body(new ForgotResponseDto(false, "Email and OTP are required", "Missing fields"));
        }

        // Verify OTP
        try {
            ForgotResponseDto response = forgotPasswordService.verifyOtp(email, otp);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ForgotResponseDto(false, "Error verifying OTP", e.getMessage()));
        }
    }

    // Update Password
    @PostMapping("/update-password")
    public ResponseEntity<ForgotResponseDto> updatePassword(@RequestBody ForgotPasswordDto forgotPasswordDto) {
        String email = forgotPasswordDto.getEmail();
        String updatePassword = forgotPasswordDto.getUpdatePassword();
        String confirmPassword = forgotPasswordDto.getConfirmPassword();

        // Validate the fields (check if they are not null or empty)
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(new ForgotResponseDto(false, "Email cannot be null or empty", "Invalid email"));
        }

        if (updatePassword == null || updatePassword.isEmpty()) {
            return ResponseEntity.badRequest().body(new ForgotResponseDto(false, "Update Password cannot be null or empty", "Missing update password"));
        }

        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(new ForgotResponseDto(false, "Confirm Password cannot be null or empty", "Missing confirm password"));
        }

        // Check if the password and confirm password match
        if (!updatePassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(new ForgotResponseDto(false, "Password and Confirm Password do not match", "Passwords mismatch"));
        }

        try {
            // Update the password in the service
            ForgotResponseDto response = forgotPasswordService.updatePassword(email, updatePassword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ForgotResponseDto(false, "Error updating password", e.getMessage()));
        }
    }
}
