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

import java.time.LocalDate;
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
        updateField(dto.getDob(), user.getDob(), "DOB", user::setDob, updatedFields);
        updateField(dto.getFatherOrSpouseName(), user.getFatherOrSpouseName(), "Father Name / Spouse Name", user::setFatherOrSpouseName, updatedFields);
        updateField(dto.getMotherName(), user.getMotherName(), "Mother Name", user::setMotherName, updatedFields);
        updateField(dto.getBloodGroup(), user.getBloodGroup(), "Blood Group", user::setBloodGroup, updatedFields);
        updateField(dto.getGender(), user.getGender(), "Gender", user::setGender, updatedFields);
        updateField(dto.getMaritalStatus(), user.getMaritalStatus(), "Marital Status", user::setMaritalStatus, updatedFields);
        updateField(firstNonNull(dto.getPersonal_email(), dto.getPersonalEmail()), user.getPersonalemail(), "Personal Email", user::setPersonalemail, updatedFields);
        if (dto.getJoining_date() != null && !Objects.equals(dto.getJoining_date(), user.getJoiningDate())) {
            user.setJoiningDate(dto.getJoining_date());
            updatedFields.put("Joining Date", dto.getJoining_date().toString());
            logger.debug("Updated field: Joining Date -> {}", dto.getJoining_date());
        }
        updateField(dto.getEmergencyContactNumber(), user.getEmergencyContactNumber(), "Emergency Contact No", user::setEmergencyContactNumber, updatedFields);
        updateField(dto.getCurrentAddress(), user.getCurrentAddress(), "Current Address", user::setCurrentAddress, updatedFields);
        updateField(dto.getPermanentAddress(), user.getPermanentAddress(), "Permanent Address", user::setPermanentAddress, updatedFields);
        updateField(firstNonNull(dto.getLinkedInUrl(), dto.getLinkedinUrl()), user.getLinkedinUrl(), "LinkedIn URL", user::setLinkedinUrl, updatedFields);
        updateDateField(dto.getDoj(), user.getDoj(), "DOJ", user::setDoj, updatedFields);
        updateField(dto.getOfficialNumber(), user.getOfficialNumber(), "Official Number", user::setOfficialNumber, updatedFields);
        updateField(dto.getOfficialEmailId(), user.getOfficialEmailId(), "Official Email ID", user::setOfficialEmailId, updatedFields);
        updateField(dto.getProbation(), user.getProbation(), "Probation", user::setProbation, updatedFields);
        updateField(dto.getReportingManager(), user.getReportingManager(), "Reporting Manager", user::setReportingManager, updatedFields);
        updateField(dto.getDepartment(), user.getDepartment(), "Department", user::setDepartment, updatedFields);
        updateField(dto.getBankName(), user.getBankName(), "Bank Name", user::setBankName, updatedFields);
        updateField(dto.getAccountNumber(), user.getAccountNumber(), "Account Number", user::setAccountNumber, updatedFields);
        updateField(dto.getBranch(), user.getBranch(), "Branch", user::setBranch, updatedFields);
        updateField(dto.getAccountHolderName(), user.getAccountHolderName(), "Account Holder Name", user::setAccountHolderName, updatedFields);
        updateField(dto.getIfscCode(), user.getIfscCode(), "IFSC Code", user::setIfscCode, updatedFields);
        updateBooleanField(dto.getIsEmployeeHavingPF(), user.getIsEmployeeHavingPF(), "Employee Having PF", user::setIsEmployeeHavingPF, updatedFields);
        updateField(dto.getUanNumber(), user.getUanNumber(), "UAN Number", user::setUanNumber, updatedFields);
        updateField(dto.getPfNumber(), user.getPfNumber(), "PF Number", user::setPfNumber, updatedFields);
        updateField(dto.getPayrollPanNumber(), user.getPayrollPanNumber(), "Payroll PAN Number", user::setPayrollPanNumber, updatedFields);
        updateField(dto.getPayrollAadharNumber(), user.getPayrollAadharNumber(), "Payroll Aadhar Number", user::setPayrollAadharNumber, updatedFields);
        updateField(firstNonNull(dto.getClearanceForm(), dto.getClearnessForm()), user.getClearnessForm(), "Clearance Form", user::setClearnessForm, updatedFields);
        updateField(dto.getFAndF(), user.getFAndF(), "F&F", user::setFAndF, updatedFields);
        updateDateField(dto.getExitFromPfDate(), user.getExitFromPfDate(), "Exit From PF Date", user::setExitFromPfDate, updatedFields);
        updateDateField(dto.getLastWorkingDay(), user.getLastWorkingDay(), "Last Working Day", user::setLastWorkingDay, updatedFields);
        updateField(dto.getPan(), user.getPan(), "PAN", user::setPan, updatedFields);
        updateField(dto.getAdhar(), user.getAdhar(), "Adhar", user::setAdhar, updatedFields);

        // 🔹 Profile photo
        if (dto.getProfilePhoto() != null && !dto.getProfilePhoto().isEmpty()) {
            try {
                byte[] photoBytes = dto.getProfilePhoto().getBytes();
                user.setProfilePhoto(photoBytes);
                user.setProfilePhotoFileName(dto.getProfilePhoto().getOriginalFilename());
                user.setProfilePhotoContentType(dto.getProfilePhoto().getContentType());
                updatedFields.put("Profile Photo", "Updated");
                logger.info("Profile photo updated for user: {}", userId);
            } catch (Exception e) {
                logger.error("Failed to save profile photo: {}", e.getMessage());
                throw new RuntimeException("Profile photo upload failed");
            }
        }

        userDao.save(user);

        List<UserDocumentDto> documents = normalizeDocuments(dto);
        if (!documents.isEmpty()) {
            for (UserDocumentDto docDto : documents) {
                try {
                    MultipartFile file = docDto.getFile();
                    if (file != null && !file.isEmpty()) {
                        UserProfileDocument document = new UserProfileDocument();
                        document.setUserId(userId);
                        document.setUserName(user.getUserName());
                        document.setDocumentType(docDto.getDocumentType());
                        document.setFileName(file.getOriginalFilename());
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

    private void updateDateField(LocalDate newValue, LocalDate oldValue, String fieldName,
                                 java.util.function.Consumer<LocalDate> setter, Map<String, String> updates) {
        if (newValue != null && !Objects.equals(newValue, oldValue)) {
            setter.accept(newValue);
            updates.put(fieldName, newValue.toString());
            logger.debug("Updated field: {} -> {}", fieldName, newValue);
        }
    }

    private void updateBooleanField(Boolean newValue, Boolean oldValue, String fieldName,
                                    java.util.function.Consumer<Boolean> setter, Map<String, String> updates) {
        if (newValue != null && !Objects.equals(newValue, oldValue)) {
            setter.accept(newValue);
            updates.put(fieldName, newValue.toString());
            logger.debug("Updated field: {} -> {}", fieldName, newValue);
        }
    }

    private String firstNonNull(String preferredValue, String fallbackValue) {
        return preferredValue != null ? preferredValue : fallbackValue;
    }

    private List<UserDocumentDto> normalizeDocuments(UserProfileUpdateDto dto) {
        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {
            return dto.getDocuments();
        }

        List<MultipartFile> files = dto.getDocumentFiles();
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> documentTypes = dto.getDocumentTypes();
        List<UserDocumentDto> documents = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file == null || file.isEmpty()) {
                continue;
            }

            UserDocumentDto document = new UserDocumentDto();
            document.setFile(file);
            if (documentTypes != null && i < documentTypes.size()) {
                document.setDocumentType(documentTypes.get(i));
            } else {
                document.setDocumentType(file.getOriginalFilename());
            }
            documents.add(document);
        }
        return documents;
    }

}

