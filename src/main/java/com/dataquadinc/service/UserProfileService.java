package com.dataquadinc.service;

import com.dataquadinc.repository.UserDao;
import com.dataquadinc.repository.UserProfileDocumentRepository;
import com.dataquadinc.dto.*;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserProfileDocument;
import com.dataquadinc.exceptions.ErrorDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;


@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);

    @Autowired
    private UserDao userDao;
    @Autowired
    private UserProfileDocumentRepository documentRepo;
    @Autowired
    private EmailService emailService;

    @Transactional
    public ApiResponse<UserProfileResponse> updateUserProfile(String userId, UserProfileUpdateDto dto) {
        logger.info("Updating user profile for userId: {}", userId);

        UserDetails user = userDao.findByUserId(userId);
        if (user == null) {
            logger.error("User not found for ID: {}", userId);
            throw new RuntimeException("User not found");
        }

        Map<String, String> updatedFields = new LinkedHashMap<>();

        // 🔹 Partial update logic
        updateField(dto.getUserName(), user.getUserName(), "Name", user::setUserName, updatedFields);
        updateField(dto.getPhoneNumber(), user.getPhoneNumber(), "Phone Number", user::setPhoneNumber, updatedFields);
        updateField(dto.getPersonalEmail(), user.getPersonalemail(), "Personal Email", user::setPersonalemail, updatedFields);
        updateField(dto.getEmergencyContactNumber(), user.getEmergencyContactNumber(), "Emergency Contact No", user::setEmergencyContactNumber, updatedFields);
        updateField(dto.getCurrentAddress(), user.getCurrentAddress(), "Current Address", user::setCurrentAddress, updatedFields);
        updateField(dto.getPermanentAddress(), user.getPermanentAddress(), "Permanent Address", user::setPermanentAddress, updatedFields);
        updateField(dto.getLinkedinUrl(), user.getLinkedinUrl(), "LinkedIn URL", user::setLinkedinUrl, updatedFields);

        // 🔹 Profile photo
        if (dto.getProfilePhoto() != null && !dto.getProfilePhoto().isEmpty()) {
            try {
                byte[] photoBytes = dto.getProfilePhoto().getBytes();
                user.setProfilePhoto(photoBytes);
                updatedFields.put("Profile Photo", "Updated");
                logger.info("Profile photo updated for user: {}", userId);
            } catch (Exception e) {
                logger.error("Failed to save profile photo: {}", e.getMessage());
                throw new RuntimeException("Profile photo upload failed");
            }
        }

        userDao.save(user);

        // 🔹 Save documents (BLOB)
        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {
            for (UserDocumentDto docDto : dto.getDocuments()) {
                try {
                    MultipartFile file = docDto.getFile();
                    if (file != null && !file.isEmpty()) {
                        UserProfileDocument document = new UserProfileDocument();
                        document.setUserId(userId);
                        document.setUserName(user.getUserName());
                        document.setDocumentType(docDto.getDocumentType());
                        document.setFileType(file.getContentType());
                        document.setDocumentData(file.getBytes());
                        documentRepo.save(document);
                        updatedFields.put("Document (" + docDto.getDocumentType() + ")", "Uploaded");
                        logger.info("Document {} uploaded successfully for user {}", docDto.getDocumentType(), userId);
                    }
                } catch (Exception e) {
                    logger.error("Error uploading document {} for user {}: {}", docDto.getDocumentType(), userId, e.getMessage());
                    throw new RuntimeException("Document upload failed");
                }
            }
        }

        // 🔹 Send emails
        if (!updatedFields.isEmpty()) {
            try {
                emailService.sendProfileUpdateEmailToUser(user, updatedFields);
                emailService.sendProfileUpdateEmailToAdmin(user, updatedFields);
            } catch (Exception e) {
                logger.warn("Email sending failed for user {}: {}", userId, e.getMessage());
            }
        }

        // 🔹 Prepare response
        List<UserProfileDocument> docs = documentRepo.findByUserId(userId);
        UserProfileResponse response = new UserProfileResponse(user, docs);
        return ApiResponse.success("Profile updated successfully", response);
    }

    public ApiResponse<UserProfileResponse> getUserProfile(String userId) {
        logger.info("Fetching user profile for userId: {}", userId);

        UserDetails user = userDao.findByUserId(userId);
        if (user == null) {
            logger.error("User not found for ID: {}", userId);
            throw new RuntimeException("User not found");
        }

        List<UserProfileDocument> docs = documentRepo.findByUserId(userId);
        UserProfileResponse response = new UserProfileResponse(user, docs);

        return ApiResponse.success("Profile fetched successfully", response);
    }

    private void updateField(String newValue, String oldValue, String fieldName,
                             java.util.function.Consumer<String> setter, Map<String, String> updates) {
        if (newValue != null && !newValue.equals(oldValue)) {
            setter.accept(newValue);
            updates.put(fieldName, newValue);
            logger.debug("Updated field: {} -> {}", fieldName, newValue);
        }
    }


}
