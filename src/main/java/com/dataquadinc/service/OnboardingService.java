package com.dataquadinc.service;

import com.dataquadinc.dto.EmployeeWithRole;
import com.dataquadinc.dto.ResponseBean;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserProfileDocument;
import com.dataquadinc.repository.UserDao;
import com.dataquadinc.repository.UserProfileDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingService.class);
    private static final int INVITE_VALID_DAYS = 7;

    @Autowired
    private UserDao userDao;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserProfileDocumentRepository documentRepo;

    @Value("${app.frontend.url:https://mymulya.com}")
    private String frontendUrl;

    public ResponseEntity<ResponseBean<Map<String, Object>>> sendInvite(String userId) {
        UserDetails user = userDao.findByUserId(userId);
        if (user == null) {
            return error("User not found", HttpStatus.NOT_FOUND);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        user.setInviteToken(token);
        user.setInviteTokenExpiry(LocalDateTime.now().plusDays(INVITE_VALID_DAYS));
        user.setInviteSentAt(LocalDateTime.now());
        user.setOnboardingStatus("INVITED");
        if (user.getStatus() == null || "ACTIVE".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("INACTIVE");
        }
        userDao.save(user);

        String inviteLink = frontendUrl.replaceAll("/$", "") + "/onboarding/" + token;
        emailService.sendOnboardingInviteEmail(user.getEmail(), user.getUserName(), inviteLink);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("email", user.getEmail());
        data.put("onboardingStatus", user.getOnboardingStatus());
        data.put("inviteSentAt", user.getInviteSentAt());
        return success("Invitation sent successfully", data);
    }

    public ResponseEntity<ResponseBean<Map<String, Object>>> validateToken(String token) {
        UserDetails user = resolveToken(token);
        if (user == null) {
            return error("Invalid or expired invitation link", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("userName", user.getUserName());
        data.put("email", user.getEmail());
        data.put("phoneNumber", user.getPhoneNumber());
        data.put("personalemail", user.getPersonalemail());
        data.put("dob", user.getDob());
        data.put("gender", user.getGender());
        data.put("currentAddress", user.getCurrentAddress());
        data.put("permanentAddress", user.getPermanentAddress());
        data.put("pan", user.getPan());
        data.put("adhar", user.getAdhar());
        data.put("onboardingStatus", user.getOnboardingStatus());
        data.put("onboardingRemarks", user.getOnboardingRemarks());
        return success("Invitation valid", data);
    }

    public ResponseEntity<ResponseBean<Map<String, Object>>> completeOnboarding(
            String token,
            String password,
            String userName,
            String phoneNumber,
            String personalemail,
            String dob,
            String gender,
            String currentAddress,
            String permanentAddress,
            String pan,
            String adhar,
            String emergencyContactNumber,
            List<MultipartFile> documents,
            List<String> documentTypes) {

        UserDetails user = resolveToken(token);
        if (user == null) {
            return error("Invalid or expired invitation link", HttpStatus.BAD_REQUEST);
        }

        if (password == null || password.trim().length() < 6) {
            return error("Password must be at least 6 characters", HttpStatus.BAD_REQUEST);
        }

        if (userName != null && !userName.isBlank()) {
            user.setUserName(userName.trim());
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            user.setPhoneNumber(phoneNumber.trim());
        }
        if (personalemail != null && !personalemail.isBlank()) {
            user.setPersonalemail(personalemail.trim());
        }
        if (dob != null) {
            user.setDob(dob);
        }
        if (gender != null) {
            user.setGender(gender);
        }
        if (currentAddress != null) {
            user.setCurrentAddress(currentAddress);
        }
        if (permanentAddress != null) {
            user.setPermanentAddress(permanentAddress);
        }
        if (pan != null) {
            user.setPan(pan);
        }
        if (adhar != null) {
            user.setAdhar(adhar);
        }
        if (emergencyContactNumber != null) {
            user.setEmergencyContactNumber(emergencyContactNumber);
        }

        String encoded = passwordEncoder.encode(password.trim());
        user.setPassword(encoded);
        user.setConfirmPassword(encoded);
        user.setOnboardingStatus("SUBMITTED");
        user.setOnboardingRemarks(null);
        user.setStatus("INACTIVE");
        userDao.save(user);

        if (documents != null) {
            for (int i = 0; i < documents.size(); i++) {
                MultipartFile file = documents.get(i);
                if (file == null || file.isEmpty()) {
                    continue;
                }
                try {
                    UserProfileDocument document = new UserProfileDocument();
                    document.setUserId(user.getUserId());
                    document.setUserName(user.getUserName());
                    document.setDocumentType(
                            documentTypes != null && i < documentTypes.size()
                                    ? documentTypes.get(i)
                                    : "ONBOARDING");
                    document.setFileName(file.getOriginalFilename());
                    document.setFileType(file.getContentType());
                    document.setDocumentData(file.getBytes());
                    documentRepo.save(document);
                } catch (IOException e) {
                    logger.error("Failed to save onboarding document for {}", user.getUserId(), e);
                    return error("Failed to upload documents", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("onboardingStatus", user.getOnboardingStatus());
        return success("Onboarding submitted successfully. HR will review your details.", data);
    }

    public ResponseEntity<ResponseBean<Map<String, Object>>> requestRemarks(String userId, String remarks) {
        UserDetails user = userDao.findByUserId(userId);
        if (user == null) {
            return error("User not found", HttpStatus.NOT_FOUND);
        }
        if (remarks == null || remarks.isBlank()) {
            return error("Remarks are required", HttpStatus.BAD_REQUEST);
        }

        String token = user.getInviteToken();
        if (token == null || token.isBlank()
                || user.getInviteTokenExpiry() == null
                || user.getInviteTokenExpiry().isBefore(LocalDateTime.now())) {
            token = UUID.randomUUID().toString().replace("-", "");
            user.setInviteToken(token);
            user.setInviteTokenExpiry(LocalDateTime.now().plusDays(INVITE_VALID_DAYS));
        }

        user.setOnboardingRemarks(remarks.trim());
        user.setOnboardingStatus("REMARKS_REQUESTED");
        user.setStatus("INACTIVE");
        userDao.save(user);

        String inviteLink = frontendUrl.replaceAll("/$", "") + "/onboarding/" + token;
        emailService.sendOnboardingRemarksEmail(user.getEmail(), user.getUserName(), remarks.trim(), inviteLink);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("onboardingStatus", user.getOnboardingStatus());
        data.put("onboardingRemarks", user.getOnboardingRemarks());
        return success("Remarks sent to candidate", data);
    }

    public ResponseEntity<ResponseBean<Map<String, Object>>> acknowledge(String userId) {
        UserDetails user = userDao.findByUserId(userId);
        if (user == null) {
            return error("User not found", HttpStatus.NOT_FOUND);
        }

        user.setStatus("ACTIVE");
        user.setOnboardingStatus("APPROVED");
        user.setOnboardingRemarks(null);
        user.setInviteToken(null);
        user.setInviteTokenExpiry(null);
        userDao.save(user);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("status", user.getStatus());
        data.put("onboardingStatus", user.getOnboardingStatus());
        data.put("placementId", user.getPlacementId());
        data.put("userName", user.getUserName());
        data.put("email", user.getEmail());
        return success("Candidate acknowledged as active employee", data);
    }

    public ResponseEntity<List<EmployeeWithRole>> pendingReview() {
        List<UserDetails> users = userDao.findAll().stream()
                .filter(u -> "Candidate".equalsIgnoreCase(u.getDesignation()))
                .filter(u -> {
                    String s = u.getOnboardingStatus();
                    return "SUBMITTED".equalsIgnoreCase(s) || "REMARKS_REQUESTED".equalsIgnoreCase(s);
                })
                .collect(Collectors.toList());

        List<EmployeeWithRole> result = users.stream()
                .map(user -> {
                    String rolesString = user.getRoles() == null ? "" : user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.joining(", "));
                    return EmployeeWithRole.fromUserDetails(user, rolesString);
                })
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return ResponseEntity.ok(result);
    }

    private UserDetails resolveToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        UserDetails user = userDao.findByInviteToken(token);
        if (user == null) {
            return null;
        }
        if (user.getInviteTokenExpiry() != null && user.getInviteTokenExpiry().isBefore(LocalDateTime.now())) {
            return null;
        }
        return user;
    }

    private ResponseEntity<ResponseBean<Map<String, Object>>> success(String message, Map<String, Object> data) {
        ResponseBean<Map<String, Object>> bean = new ResponseBean<>();
        bean.setSuccess(true);
        bean.setMessage(message);
        bean.setData(data);
        bean.setError(null);
        return ResponseEntity.ok(bean);
    }

    private ResponseEntity<ResponseBean<Map<String, Object>>> error(String message, HttpStatus status) {
        ResponseBean<Map<String, Object>> bean = new ResponseBean<>();
        bean.setSuccess(false);
        bean.setMessage(message);
        bean.setData(null);
        return new ResponseEntity<>(bean, status);
    }
}
