package com.dataquadinc.controller;

import com.dataquadinc.dto.ForgotResponseDto;
import com.dataquadinc.dto.UserVerifyDto;
import com.dataquadinc.service.UserVerifyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/users")
public class UserVerifyController {

    private final UserVerifyService userService;

    public UserVerifyController(UserVerifyService userService) {
        this.userService = userService;
    }

    // Send OTP to Email
    @PostMapping("/sendOtp")
    public ResponseEntity<ForgotResponseDto> sendOtp(@RequestParam String email) {
        // Check if email is valid (you can use a regex for a more thorough check)
        if (email == null || email.isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ResponseEntity.badRequest().body(new ForgotResponseDto(false, "Invalid email format", "Please provide a valid email address"));
        }

        // Call service to send OTP
        ForgotResponseDto response = userService.sendOtp(email);
        return ResponseEntity.ok(response);
    }

    // Verify OTP
    @PostMapping("/verifyOtp")
    public ResponseEntity<ForgotResponseDto> verifyOtp(@RequestBody UserVerifyDto userDTO) {
        // Ensure both email and OTP are provided
        String email = userDTO.getEmail();
        String otp = userDTO.getOtp();
        if (email == null || otp == null || email.isEmpty() || otp.isEmpty()) {
            return ResponseEntity.badRequest().body(new ForgotResponseDto(false, "Email and OTP are required", "Missing fields"));
        }

        // Call service to verify OTP
        ForgotResponseDto response = userService.verifyOtp(userDTO);
        return ResponseEntity.ok(response);
    }
}
