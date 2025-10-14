package com.dataquadinc.controller;

import com.dataquadinc.dto.ApiResponse;
import com.dataquadinc.dto.UserProfileUpdateDto;
import com.dataquadinc.dto.UserProfileResponse;
import com.dataquadinc.exceptions.ErrorDto;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserProfileDocument;
import com.dataquadinc.repository.UserDao;
import com.dataquadinc.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private com.dataquadinc.repository.UserProfileDocumentRepository documentRepo;

    @PutMapping("/update/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserProfile(
            @PathVariable String userId,
            @ModelAttribute UserProfileUpdateDto dto) {
        logger.info("Received request to update user profile for userId: {}", userId);
        try {
            ApiResponse<UserProfileResponse> response = userProfileService.updateUserProfile(userId, dto);
            logger.info("Profile updated successfully for userId: {}", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating profile for userId {}: {}", userId, e.getMessage(), e);
            ErrorDto error = new ErrorDto(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to update profile", null, error));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable String userId) {
        logger.info("Fetching user profile for userId: {}", userId);
        try {
            ApiResponse<UserProfileResponse> response = userProfileService.getUserProfile(userId);
            logger.info("Profile fetched successfully for userId: {}", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching profile for userId {}: {}", userId, e.getMessage(), e);
            ErrorDto error = new ErrorDto(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to fetch profile", null, error));
        }
    }

    @GetMapping("/{userId}/photo")
    public ResponseEntity<byte[]> getUserProfilePhoto(@PathVariable String userId) {
        UserDetails user = userDao.findByUserId(userId);
        if (user == null || user.getProfilePhoto() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .body(user.getProfilePhoto());
    }

    @GetMapping("/{userId}/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadUserDocument(@PathVariable String userId, @PathVariable Long documentId) {
        UserProfileDocument document = documentRepo.findById(documentId).orElse(null);
        if (document == null || !document.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header("Content-Type", document.getFileType())
                .header("Content-Disposition", "attachment; filename=\"" + document.getDocumentType() + "\"")
                .body(document.getDocumentData());
    }

}
