package com.dataquadinc.controller;

import com.dataquadinc.dto.EmployeeWithRole;
import com.dataquadinc.dto.ResponseBean;
import com.dataquadinc.service.OnboardingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000",
        "https://mymulya.com", "http://localhost:3000", "http://192.168.0.135:8080", "http://192.168.0.135",
        "http://154.210.288.26", "http://192.168.0.203:3000", "http://192.168.0.167:3000"})
@RestController
@RequestMapping("/users/onboarding")
public class OnboardingController {

    @Autowired
    private OnboardingService onboardingService;

    @PostMapping("/invite/{userId}")
    public ResponseEntity<ResponseBean<Map<String, Object>>> sendInvite(@PathVariable String userId) {
        return onboardingService.sendInvite(userId);
    }

    @GetMapping("/validate/{token}")
    public ResponseEntity<ResponseBean<Map<String, Object>>> validateToken(@PathVariable String token) {
        return onboardingService.validateToken(token);
    }

    @PostMapping(value = "/complete/{token}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseBean<Map<String, Object>>> completeOnboarding(
            @PathVariable String token,
            @RequestParam("password") String password,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "personalemail", required = false) String personalemail,
            @RequestParam(value = "dob", required = false) String dob,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "currentAddress", required = false) String currentAddress,
            @RequestParam(value = "permanentAddress", required = false) String permanentAddress,
            @RequestParam(value = "pan", required = false) String pan,
            @RequestParam(value = "adhar", required = false) String adhar,
            @RequestParam(value = "emergencyContactNumber", required = false) String emergencyContactNumber,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents,
            @RequestParam(value = "documentTypes", required = false) List<String> documentTypes) {
        return onboardingService.completeOnboarding(
                token, password, userName, phoneNumber, personalemail, dob, gender,
                currentAddress, permanentAddress, pan, adhar, emergencyContactNumber,
                documents, documentTypes);
    }

    @PostMapping("/remarks/{userId}")
    public ResponseEntity<ResponseBean<Map<String, Object>>> requestRemarks(
            @PathVariable String userId,
            @RequestBody Map<String, String> body) {
        return onboardingService.requestRemarks(userId, body != null ? body.get("remarks") : null);
    }

    @PostMapping("/acknowledge/{userId}")
    public ResponseEntity<ResponseBean<Map<String, Object>>> acknowledge(@PathVariable String userId) {
        return onboardingService.acknowledge(userId);
    }

    @GetMapping("/pending-review")
    public ResponseEntity<List<EmployeeWithRole>> pendingReview() {
        return onboardingService.pendingReview();
    }
}
