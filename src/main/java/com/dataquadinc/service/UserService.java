
package com.dataquadinc.service;

import com.dataquadinc.dto.*;
import com.dataquadinc.exceptions.DateRangeValidationException;
import com.dataquadinc.exceptions.NoSuchUserException;
import com.dataquadinc.exceptions.UserNotFoundException;
import com.dataquadinc.exceptions.ValidationException;
import com.dataquadinc.mapper.UserMapper;
import com.dataquadinc.model.*;
import com.dataquadinc.repository.*;


import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;

@Service
public class UserService {
    @Autowired
    private UserDao userDao;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RolesDao rolesDao;

    @Autowired
    private UserProfileDocumentRepository documentRepo;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AttendanceMonthConfigRepository attendanceMonthConfigRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    public ResponseEntity<ResponseBean<UserResponse>> registerUser(UserDto userDto) {
        Map<String, String> errors = new HashMap<>();

        logger.info("New User Registering ...{}", userDto.getUserId());

        // Check for existing email
        if (userDao.findByEmail(userDto.getEmail()) != null) {
            logger.warn("Email {} is already in use", userDto.getEmail());
            errors.put("errormessage", userDto.getEmail() + " is already in use");
        }

        // Check for existing userId
        if (userDao.findByUserId(userDto.getUserId()) != null) {
            logger.warn("UserId {} already exists", userDto.getUserId());
            errors.put("errorMessage", userDto.getUserId() + " already exists. Please log in");
        }

        if (!errors.isEmpty()) {
            logger.error("User registration errors: {}", errors);
            throw new ValidationException(errors);
        }

        // Save plain password before encoding for email
        String plainPassword = userDto.getPassword();

        // Encrypt passwords
        userDto.setPassword(passwordEncoder.encode(plainPassword));
        userDto.setConfirmPassword(passwordEncoder.encode(userDto.getConfirmPassword()));

        // Map DTO to entity
        UserDetails user = userMapper.toEntity(userDto);
        user.setEncryptionKey("MyMulya@1234");
        user.setPrimarySuperAdmin(false);

        Set<Roles> roles = userDto.getRoles().stream()
                .map(role -> {
                    try {
                        return rolesDao.findByName(role)
                                .orElseThrow(() -> new ValidationException(Map.of("role", "roleNotFound")));
                    } catch (ValidationException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());

        user.setRoles(roles);

        UserDetails dbUser = userDao.save(user);
        logger.info("User {} saved successfully", dbUser.getUserId());

        UserResponse res = new UserResponse();
        res.setUserName(dbUser.getUserName());
        res.setUserId(dbUser.getUserId());
        res.setEmail(dbUser.getEmail());

        // Check and send email for EMPLOYEE role only
        boolean onlyEmployee = roles.size() == 1 && roles.stream()
                .allMatch(r -> UserType.EMPLOYEE.name().equalsIgnoreCase(r.getName().name()));

        if (onlyEmployee) {
            try {
                logger.info("Sending credentials email to {}", dbUser.getEmail());
                emailService.sendPasswordEmailHtml(dbUser.getEmail(), dbUser.getUserName(), plainPassword);
                logger.info("Email sent successfully to {}", dbUser.getEmail());
            } catch (Exception e) {
                logger.error("Failed to send email to {}: {}", dbUser.getEmail(), e.getMessage(), e);
                // Optionally: handle or rethrow exception as needed
            }
        } else {
            logger.info("Not sending email; user role is not EMPLOYEE only");
        }

        ResponseBean<UserResponse> resp = new ResponseBean<>();
        resp.setSuccess(true);
        resp.setMessage("Created Successfully");
        resp.setData(res);
        resp.setError(null);

        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    public ResponseEntity<List<EmployeeWithRole>> getEmployeesWithFlexibleRoleFilter(
            String userId, String roleName, String excludeRoleName, String entity) {

        UserType includeRole = null;
        UserType excludeRole = null;
        String requestedEntity = (entity == null || entity.isBlank()) ? "IN" : entity.trim().toUpperCase();

        // Parse roleName
        if (roleName != null && !roleName.isBlank()) {
            try {
                includeRole = UserType.fromValue(roleName);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid role name provided: {}", roleName);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        // Parse excludeRoleName
        if (excludeRoleName != null && !excludeRoleName.isBlank()) {
            try {
                excludeRole = UserType.fromValue(excludeRoleName);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid exclude role name provided: {}", excludeRoleName);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        List<UserDetails> users;

        if (excludeRole != null) {
            // ✅ Proper "NOT EXISTS" exclusion, also applies entity='IN'
            users = userDao.findByUserIdAndRoleNot(userId, excludeRole, requestedEntity);
            logger.info("Fetched {} users with userId={} excluding role={}, entity={}",
                    users.size(), userId, excludeRole, requestedEntity);

        } else if (includeRole != null) {
            // ✅ Include specific role, entity='IN'
            users = userDao.findByUserIdAndRole(userId, includeRole, requestedEntity);
            logger.info("Fetched {} users with userId={} and role={}, entity={}",
                    users.size(), userId, includeRole, requestedEntity);

        } else {
            // ✅ No role filter, still entity='IN'
            users = (userId == null || userId.isBlank())
                    ? userDao.findAllActiveNonTestUsersByEntity(requestedEntity)
                    : userDao.findByUserIdAndRole(userId, null, requestedEntity);

            logger.info("Fetched {} users with userId={}, no role filter, entity={}",
                    users.size(), userId, requestedEntity);
        }

        if (users.isEmpty()) {
            logger.info("No users found matching the filters.");
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        List<EmployeeWithRole> employeeRoles = users.stream()
                .map(user -> {
                    String rolesString = user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.joining(", "));

                    return EmployeeWithRole.fromUserDetails(user, rolesString);
                })
                .collect(Collectors.toList());

        logger.info("Returning {} employee records in response", employeeRoles.size());
        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    public ResponseEntity<List<EmployeeWithRole>> findAllActiveInternal() {

        List<UserDetails> users = userDao.findAllActiveNotExternalUser();

        List<EmployeeWithRole> employeeRoles = users.stream()
                .map(user -> {
                    String rolesString = user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.joining(", "));

                    return EmployeeWithRole.fromUserDetails(user, rolesString);
                })
                .collect(Collectors.toList());

        logger.info("Returning {} employee records in response", employeeRoles.size());
        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    public ResponseEntity<List<EmployeeWithRole>> findAllActiveExternal() {

        List<UserDetails> users = userDao.findAllActiveExternalUser();

        List<EmployeeWithRole> employeeRoles = users.stream()
                .map(user -> {
                    String rolesString = user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.joining(", "));

                    return EmployeeWithRole.fromUserDetails(user, rolesString);
                })
                .collect(Collectors.toList());

        logger.info("Returning {} employee records in response", employeeRoles.size());
        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    public ResponseEntity<List<EmployeeWithRole>> findAllInActiveInternal() {

        List<UserDetails> users = userDao.findAllInActiveNotExternalUser();

        List<EmployeeWithRole> employeeRoles = users.stream()
                .map(user -> {
                    String rolesString = user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.joining(", "));

                    return EmployeeWithRole.fromUserDetails(user, rolesString);
                })
                .collect(Collectors.toList());

        logger.info("Returning {} employee records in response", employeeRoles.size());
        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    public ResponseEntity<List<EmployeeWithRole>> findAllInActiveExternal() {

        List<UserDetails> users = userDao.findAllInActiveExternalUser();

        List<EmployeeWithRole> employeeRoles = users.stream()
                .map(user -> {
                    String rolesString = user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.joining(", "));

                    return EmployeeWithRole.fromUserDetails(user, rolesString);
                })
                .collect(Collectors.toList());

        logger.info("Returning {} employee records in response", employeeRoles.size());
        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    public ResponseEntity<ResponseBean<UserResponse>> updateUser(String userId, UserDto userDto) {
        Map<String, String> errors = new HashMap<>();

        // Check if the user exists
        UserDetails existingUser = userDao.findByUserId(userId);
        if (existingUser == null) {
            ResponseBean<UserResponse> resp = new ResponseBean<>();
            resp.setSuccess(false);
            resp.setMessage("User not found");
            return new ResponseEntity<>(resp, HttpStatus.NOT_FOUND);
        }

        // Check if userName is provided and valid
        if (userDto.getUserName() == null || userDto.getUserName().isEmpty()) {
            errors.put("userName", "User name is required and cannot be null or empty");
        }

        // Check if personalemail is provided and valid
        if (userDto.getPersonalemail() == null || userDto.getPersonalemail().isEmpty()) {
            errors.put("personalemail", "Personal email is required and cannot be null or empty");
        }

        if (!errors.isEmpty()) {
            ResponseBean<UserResponse> resp = new ResponseBean<>();
            resp.setSuccess(false);
            resp.setMessage("Validation failed");
            return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
        }

        // Update the details (for example, the email, user name, etc.)
        existingUser.setUserName(userDto.getUserName());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setStatus(userDto.getStatus());
        existingUser.setGender(userDto.getGender());
        existingUser.setMaritalStatus(userDto.getMaritalStatus());
        existingUser.setFatherOrSpouseName(userDto.getFatherOrSpouseName());
        existingUser.setMotherName(userDto.getMotherName());
        existingUser.setBloodGroup(userDto.getBloodGroup());
        existingUser.setDesignation(userDto.getDesignation());
        existingUser.setDob(userDto.getDob());
        existingUser.setPersonalemail(userDto.getPersonalemail());  // Ensure this is not null or empty
        existingUser.setJoiningDate(userDto.getJoiningDate());
        existingUser.setPhoneNumber(userDto.getPhoneNumber());
        existingUser.setEntity(userDto.getEntity());
        existingUser.setPan(userDto.getPan());
        existingUser.setAdhar(userDto.getAdhar());
        existingUser.setCurrentAddress(userDto.getCurrentAddress());
        existingUser.setPermanentAddress(userDto.getPermanentAddress());
        existingUser.setDoj(userDto.getDoj());
        existingUser.setOfficialNumber(userDto.getOfficialNumber());
        existingUser.setOfficialEmailId(userDto.getOfficialEmailId());
        existingUser.setProbation(userDto.getProbation());
        existingUser.setReportingManager(userDto.getReportingManager());
        existingUser.setDepartment(userDto.getDepartment());
        existingUser.setBankName(userDto.getBankName());
        existingUser.setAccountNumber(userDto.getAccountNumber());
        existingUser.setBranch(userDto.getBranch());
        existingUser.setAccountHolderName(userDto.getAccountHolderName());
        existingUser.setIfscCode(userDto.getIfscCode());
        if (userDto.getIsEmployeeHavingPF() != null) {
            existingUser.setIsEmployeeHavingPF(userDto.getIsEmployeeHavingPF());
        }
        existingUser.setUanNumber(userDto.getUanNumber());
        existingUser.setPfNumber(userDto.getPfNumber());
        existingUser.setEsiNumber(userDto.getEsiNumber());
        existingUser.setIsEmployeeHavingESI(userDto.getIsEmployeeHavingESI());
        existingUser.setPayrollPanNumber(userDto.getPayrollPanNumber());
        existingUser.setPayrollAadharNumber(userDto.getPayrollAadharNumber());
        existingUser.setClearnessForm(userDto.getClearanceForm() != null ? userDto.getClearanceForm() : userDto.getClearnessForm());
        existingUser.setFAndF(userDto.getFAndF());
        existingUser.setExitFromPfDate(userDto.getExitFromPfDate());
        existingUser.setLastWorkingDay(userDto.getLastWorkingDay());
        if (userDto.getIsEditable() != null) {
            existingUser.setIsEditable(userDto.getIsEditable());
        }
        if (userDto.getLinkedInUrl() != null) {
            existingUser.setLinkedinUrl(userDto.getLinkedInUrl());
        } else {
            existingUser.setLinkedinUrl(userDto.getLinkedinUrl());
        }
        if (userDto.getEmergencyContactNumber() != null) {
            existingUser.setEmergencyContactNumber(userDto.getEmergencyContactNumber());
        } else {
            existingUser.setEmergencyContactNumber(userDto.getEmergencyContactNo());
        }

        // If password is provided, encode it and update it
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        // Handle roles update
        Set<Roles> roles = userDto.getRoles().stream()
                .map(role -> {
                    try {
                        return rolesDao.findByName(role)
                                .orElseThrow(() -> new ValidationException(Map.of("role", "roleNotFound")));
                    } catch (ValidationException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());
        existingUser.setRoles(roles);

        // Save the updated user
        UserDetails updatedUser = userDao.save(existingUser);
        System.out.println("Saved UserName: " + updatedUser.getUserName());

        // Prepare response
        UserResponse userResponse = new UserResponse();
        userResponse.setUserName(updatedUser.getUserName());
        userResponse.setUserId(updatedUser.getUserId());
        userResponse.setEmail(updatedUser.getEmail());

        // Prepare the response bean
        ResponseBean<UserResponse> responseBean = new ResponseBean<>();
        responseBean.setSuccess(true);
        responseBean.setMessage("User updated successfully");
        responseBean.setData(userResponse);
        responseBean.setError(null);

        return new ResponseEntity<>(responseBean, HttpStatus.OK);
    }

    public ResponseEntity<ResponseBean<UserResponse>> updateUserMultipart(
            String userId,
            MultiValueMap<String, String> formFields,
            MultipartFile profilePhoto,
            List<MultipartFile> documentFiles,
            List<MultipartFile> documents) {

        UserDetails existingUser = userDao.findByUserId(userId);
        if (existingUser == null) {
            ResponseBean<UserResponse> resp = new ResponseBean<>();
            resp.setSuccess(false);
            resp.setMessage("User not found");
            return new ResponseEntity<>(resp, HttpStatus.NOT_FOUND);
        }

        if (formFields != null) {
            updateFieldIfSubmitted(formFields, "userName", existingUser::setUserName);
            updateFieldIfSubmitted(formFields, "email", existingUser::setEmail);
            updateFieldIfSubmitted(formFields, "status", existingUser::setStatus);
            updateFieldIfSubmitted(formFields, "gender", existingUser::setGender);
            updateFieldIfSubmitted(formFields, "maritalStatus", existingUser::setMaritalStatus);
            updateFieldIfSubmitted(formFields, "fatherOrSpouseName", existingUser::setFatherOrSpouseName);
            updateFieldIfSubmitted(formFields, "motherName", existingUser::setMotherName);
            updateFieldIfSubmitted(formFields, "bloodGroup", existingUser::setBloodGroup);
            updateFieldIfSubmitted(formFields, "designation", existingUser::setDesignation);
            updateFieldIfSubmitted(formFields, "dob", existingUser::setDob);
            updateFieldIfSubmitted(formFields, "personalemail", existingUser::setPersonalemail);
            updateFieldIfSubmitted(formFields, "phoneNumber", existingUser::setPhoneNumber);
            updateFieldIfSubmitted(formFields, "entity", existingUser::setEntity);
            updateFieldIfSubmitted(formFields, "pan", existingUser::setPan);
            updateFieldIfSubmitted(formFields, "adhar", existingUser::setAdhar);
            updateFieldIfSubmitted(formFields, "currentAddress", existingUser::setCurrentAddress);
            updateFieldIfSubmitted(formFields, "permanentAddress", existingUser::setPermanentAddress);
            updateFieldIfSubmitted(formFields, "emergencyContactNumber", existingUser::setEmergencyContactNumber);
            updateFieldIfSubmitted(formFields, "emergencyContactNo", existingUser::setEmergencyContactNumber);
            updateFieldIfSubmitted(formFields, "officialNumber", existingUser::setOfficialNumber);
            updateFieldIfSubmitted(formFields, "officialEmailId", existingUser::setOfficialEmailId);
            updateFieldIfSubmitted(formFields, "probation", existingUser::setProbation);
            updateFieldIfSubmitted(formFields, "reportingManager", existingUser::setReportingManager);
            updateFieldIfSubmitted(formFields, "department", existingUser::setDepartment);
            updateFieldIfSubmitted(formFields, "linkedInUrl", existingUser::setLinkedinUrl);
            updateFieldIfSubmitted(formFields, "linkedinUrl", existingUser::setLinkedinUrl);
            updateFieldIfSubmitted(formFields, "bankName", existingUser::setBankName);
            updateFieldIfSubmitted(formFields, "accountNumber", existingUser::setAccountNumber);
            updateFieldIfSubmitted(formFields, "branch", existingUser::setBranch);
            updateFieldIfSubmitted(formFields, "accountHolderName", existingUser::setAccountHolderName);
            updateFieldIfSubmitted(formFields, "ifscCode", existingUser::setIfscCode);
            String isEmployeeHavingPF = submittedValue(formFields, "isEmployeeHavingPF");
            if (isEmployeeHavingPF != null) {
                existingUser.setIsEmployeeHavingPF(Boolean.parseBoolean(isEmployeeHavingPF));
            }
            updateFieldIfSubmitted(formFields, "uanNumber", existingUser::setUanNumber);
            updateFieldIfSubmitted(formFields, "pfNumber", existingUser::setPfNumber);
            updateFieldIfSubmitted(formFields, "esiNumber", existingUser::setEsiNumber);
            String isEmployeeHavingESI = submittedValue(formFields, "isEmployeeHavingESI");
            if (isEmployeeHavingESI != null) {
                existingUser.setIsEmployeeHavingESI(Boolean.parseBoolean(isEmployeeHavingESI));
            }
            updateFieldIfSubmitted(formFields, "payrollPanNumber", existingUser::setPayrollPanNumber);
            updateFieldIfSubmitted(formFields, "payrollAadharNumber", existingUser::setPayrollAadharNumber);
            updateFieldIfSubmitted(formFields, "clearanceForm", existingUser::setClearnessForm);
            updateFieldIfSubmitted(formFields, "clearnessForm", existingUser::setClearnessForm);
            updateFieldIfSubmitted(formFields, "fAndF", existingUser::setFAndF);

            String joiningDate = submittedValue(formFields, "joiningDate");
            if (joiningDate != null) {
                existingUser.setJoiningDate(LocalDate.parse(joiningDate));
            }

            String doj = submittedValue(formFields, "doj");
            if (doj != null) {
                existingUser.setDoj(LocalDate.parse(doj));
            }

            String exitFromPfDate = submittedValue(formFields, "exitFromPfDate");
            if (exitFromPfDate != null) {
                existingUser.setExitFromPfDate(LocalDate.parse(exitFromPfDate));
            }

            String lastWorkingDay = submittedValue(formFields, "lastWorkingDay");
            if (lastWorkingDay != null) {
                existingUser.setLastWorkingDay(LocalDate.parse(lastWorkingDay));
            }

            String password = submittedValue(formFields, "password");
            if (password != null) {
                existingUser.setPassword(passwordEncoder.encode(password));
            }

            List<String> roleValues = formFields.containsKey("roles") ? formFields.get("roles") : formFields.get("role");
            if (roleValues != null && !roleValues.isEmpty()) {
                Set<Roles> roles = roleValues.stream()
                        .flatMap(value -> Arrays.stream(value.split(",")))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .map(value -> rolesDao.findByName(UserType.fromValue(value))
                                .orElseThrow(() -> new ValidationException(Map.of("role", "roleNotFound"))))
                        .collect(Collectors.toSet());
                if (!roles.isEmpty()) {
                    existingUser.setRoles(roles);
                }
            }
        }

        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            try {
                existingUser.setProfilePhoto(profilePhoto.getBytes());
                existingUser.setProfilePhotoFileName(profilePhoto.getOriginalFilename());
                existingUser.setProfilePhotoContentType(profilePhoto.getContentType());
            } catch (IOException e) {
                ResponseBean<UserResponse> resp = new ResponseBean<>();
                resp.setSuccess(false);
                resp.setMessage("Profile photo upload failed");
                return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
            }
        }

        deleteUserProfileDocuments(userId, formFields);

        List<MultipartFile> filesToSave = mergeDocumentFiles(documentFiles, documents);
        if (!filesToSave.isEmpty()) {
            try {
                saveUserProfileDocuments(existingUser, formFields, filesToSave);
            } catch (IOException e) {
                ResponseBean<UserResponse> resp = new ResponseBean<>();
                resp.setSuccess(false);
                resp.setMessage("Document upload failed");
                return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
            }
        }

        UserDetails updatedUser = userDao.save(existingUser);
        return new ResponseEntity<>(buildUserUpdateResponse(updatedUser, "User updated successfully"), HttpStatus.OK);
    }

    private List<MultipartFile> mergeDocumentFiles(List<MultipartFile> documentFiles, List<MultipartFile> documents) {
        List<MultipartFile> mergedFiles = new ArrayList<>();
        if (documentFiles != null) {
            mergedFiles.addAll(documentFiles);
        }
        if (documents != null) {
            mergedFiles.addAll(documents);
        }
        return mergedFiles.stream()
                .filter(file -> file != null && !file.isEmpty())
                .collect(Collectors.toList());
    }

    private void saveUserProfileDocuments(
            UserDetails user,
            MultiValueMap<String, String> formFields,
            List<MultipartFile> documentFiles) throws IOException {

        List<String> documentTypes = documentTypes(formFields);
        for (int i = 0; i < documentFiles.size(); i++) {
            MultipartFile file = documentFiles.get(i);
            UserProfileDocument document = new UserProfileDocument();
            document.setUserId(user.getUserId());
            document.setUserName(user.getUserName());
            document.setDocumentType(resolveDocumentType(documentTypes, i, file));
            document.setFileName(file.getOriginalFilename());
            document.setFileType(file.getContentType());
            document.setDocumentData(file.getBytes());
            documentRepo.save(document);
        }
    }

    private List<String> documentTypes(MultiValueMap<String, String> formFields) {
        if (formFields == null) {
            return Collections.emptyList();
        }
        if (formFields.containsKey("documentTypes")) {
            return formFields.get("documentTypes");
        }
        if (formFields.containsKey("documentType")) {
            return formFields.get("documentType");
        }
        return Collections.emptyList();
    }

    private String resolveDocumentType(List<String> documentTypes, int index, MultipartFile file) {
        if (documentTypes != null && index < documentTypes.size()) {
            String documentType = documentTypes.get(index);
            if (documentType != null && !documentType.isBlank()) {
                return documentType;
            }
        }
        return file.getOriginalFilename();
    }

    private void deleteUserProfileDocuments(String userId, MultiValueMap<String, String> formFields) {
        if (formFields == null) {
            return;
        }

        List<String> deleteDocumentIds = formFields.containsKey("deleteDocumentIds")
                ? formFields.get("deleteDocumentIds")
                : formFields.get("deleteDocIds");

        if (deleteDocumentIds == null || deleteDocumentIds.isEmpty()) {
            return;
        }

        deleteDocumentIds.stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::valueOf)
                .forEach(documentId ->
                        documentRepo.findById(documentId).ifPresent(document -> {
                            if (userId.equals(document.getUserId())) {
                                documentRepo.delete(document);
                            }
                        })
                );
    }

    private void updateFieldIfSubmitted(
            MultiValueMap<String, String> formFields,
            String fieldName,
            java.util.function.Consumer<String> setter) {
        String value = submittedValue(formFields, fieldName);
        if (value != null) {
            setter.accept(value);
        }
    }

    private String submittedValue(MultiValueMap<String, String> formFields, String fieldName) {
        if (!formFields.containsKey(fieldName)) {
            return null;
        }
        String value = formFields.getFirst(fieldName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return null;
    }

    private ResponseBean<UserResponse> buildUserUpdateResponse(UserDetails updatedUser, String message) {
        UserResponse userResponse = new UserResponse();
        userResponse.setUserName(updatedUser.getUserName());
        userResponse.setUserId(updatedUser.getUserId());
        userResponse.setEmail(updatedUser.getEmail());

        ResponseBean<UserResponse> responseBean = new ResponseBean<>();
        responseBean.setSuccess(true);
        responseBean.setMessage(message);
        responseBean.setData(userResponse);
        responseBean.setError(null);
        return responseBean;
    }

    public ResponseEntity<ResponseBean<UserResponse>> deleteUser(String userId) {
        // Check if the user exists
        UserDetails user = userDao.findByUserId(userId);
        if (user == null) {
            ResponseBean<UserResponse> response = new ResponseBean<>();
            response.setSuccess(false);
            response.setMessage("User not found");
            response.setData(null);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        UserResponse userResponse = new UserResponse();
        userResponse.setUserId(user.getUserId());
        userResponse.setUserName(user.getUserName());
        userResponse.setEmail(user.getEmail());

        // Delete the user from the database
        userDao.delete(user);


        // Prepare the response
        ResponseBean<UserResponse> response = new ResponseBean<>();
        response.setSuccess(true);
        response.setMessage("User deleted successfully");
        response.setData(userResponse);
        response.setError(null);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public UserDetails getRecruiterById(String userId) {
        UserDetails recruiter = userDao.findByUserId(userId);
        if (recruiter == null) {
            throw new UserNotFoundException("Recruiter not found with ID: " + userId);
        }
        return recruiter;
    }

    // Method to get total submission count across all clients and jobs
    public long getTotalSubmissionsAcrossAllClientsAndJobs() {
        // Calling the query that counts all submissions across all job IDs and clients
        long totalSubmissions = userDao.countAllSubmissionsAcrossAllJobsAndClients();

        System.out.println("Total Submissions across all jobs and clients: " + totalSubmissions);

        return totalSubmissions;
    }


    public List<BdmEmployeeDTO> getAllBdmEmployees() {

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.withDayOfMonth(1);
        List<UserDetails> users = userDao.findBdmEmployees();  // Get BDM employees

        System.out.println("Total BDM employees found: " + users.size());

        return users.stream().map(user -> {
            String userId = user.getUserId();
            String userName = user.getUserName();

            System.out.println("\n==== Processing BDM: " + userName + " (ID: " + userId + ") ====");

            // Get the user's role
            String roleName = Optional.ofNullable(user.getRoles())
                    .flatMap(roles -> roles.stream()
                            .map(role -> role.getName().name())
                            .findFirst())
                    .orElse("No Role");

            // ✅ Count Clients (based on onboarding)
            long clientCount = userDao.countClientsByUserIdAndDateRange(userId, startDate, endDate);
            System.out.println("Client Count: " + clientCount);

            // ✅ Get client names for this BDM
            List<String> clientNames = userDao.findClientNamesByUserIdAndDateRange(userId, startDate, endDate);
            System.out.println("Client Names for this BDM: " + clientNames);

            // Initialize counters
            long submissionCount = 0;
            long interviewCount = 0;
            long placementCount = 0;
            long requirementsCount = 0; // ✅ New counter for requirements

            // If there are clients associated with this BDM
            if (!clientNames.isEmpty()) {
                for (String clientName : clientNames) {
                    System.out.println("Processing Client: " + clientName);

                    // ✅ Count ALL submissions for this client (across ALL job IDs)
                    submissionCount += userDao.countAllSubmissionsByClientNameAndDateRange(clientName, startDate, endDate); // Updated method for count
                    System.out.println("Total Submission Count: " + submissionCount + " for Client: '" + clientName + "'");

                    // ✅ Count ALL Interviews for this client (across ALL job IDs)
                    interviewCount += userDao.countAllInterviewsByClientNameAndDateRange(clientName, startDate, endDate);
                    System.out.println("Total Interview Count: " + interviewCount + " for Client: '" + clientName + "'");

                    // ✅ Count ALL Placements for this client (across ALL job IDs)
                    placementCount += userDao.countAllPlacementsByClientNameAndDateRange(clientName, startDate, endDate);
                    System.out.println("Total Placement Count: " + placementCount + " for Client: '" + clientName + "'");

                    // ✅ Count ALL Requirements for this client
                    requirementsCount += userDao.countRequirementsByClientNameAndDateRange(clientName, startDate, endDate);
                    System.out.println("Total Requirements Count: " + requirementsCount + " for Client: '" + clientName + "'");
                }
            }

            // Return DTO for BDM employee with all relevant counts
            return new BdmEmployeeDTO(
                    userId,
                    userName,
                    roleName,
                    user.getEmail(),
                    user.getStatus(),
                    clientCount,
                    requirementsCount,  // Moved requirementsCount after clientCount
                    submissionCount,  // Now submissionCount includes the total submissions for the BDM
                    interviewCount,
                    placementCount
            );
        }).collect(Collectors.toList());
    }

    public List<EmployeeWithRole> getEmployeesByJoiningDateRange(LocalDate startDate, LocalDate endDate) {
        // ✅ Date range validations

        if (startDate.isAfter(endDate)) {
            throw new DateRangeValidationException("Start date must not be after end date.");
        }

        List<UserDetails> users = userDao.findEmployeesByJoiningDateRange(startDate, endDate);

        return users.stream().map(user -> {
            String roleName = Optional.ofNullable(user.getRoles())
                    .flatMap(roles -> roles.stream()
                            .map(role -> role.getName().name())
                            .findFirst())
                    .orElse("No Role");

            return EmployeeWithRole.fromUserDetails(user, roleName);
        }).collect(Collectors.toList());
    }

    public List<BdmEmployeeDTO> getAllBdmEmployeesDateFilter(LocalDate startDate, LocalDate endDate) {
        List<UserDetails> users = userDao.findBdmEmployees();  // Get BDM employees

        System.out.println("Total BDM employees found: " + users.size());

        return users.stream().map(user -> {
            String userId = user.getUserId();
            String userName = user.getUserName();

            System.out.println("\n==== Processing BDM: " + userName + " (ID: " + userId + ") ====");

            // Get the user's role
            String roleName = Optional.ofNullable(user.getRoles())
                    .flatMap(roles -> roles.stream()
                            .map(role -> role.getName().name())
                            .findFirst())
                    .orElse("No Role");

            // ✅ Count Clients (based on onboarding)
            long clientCount = userDao.countClientsByUserIdAndDateRange(userId, startDate, endDate);
            System.out.println("Client Count: " + clientCount);

            // ✅ Get client names for this BDM
            List<String> clientNames = userDao.findClientNamesByUserIdAndDateRange(userId, startDate, endDate);
            System.out.println("Client Names for this BDM: " + clientNames);

            // Initialize counters
            long submissionCount = 0;
            long interviewCount = 0;
            long placementCount = 0;
            long requirementsCount = 0; // ✅ New counter for requirements

            // If there are clients associated with this BDM
            if (!clientNames.isEmpty()) {
                for (String clientName : clientNames) {
                    System.out.println("Processing Client: " + clientName);

                    // ✅ Count ALL submissions for this client (across ALL job IDs)
                    submissionCount += userDao.countAllSubmissionsByClientNameAndDateRange(clientName, startDate, endDate); // Updated method for count
                    System.out.println("Total Submission Count: " + submissionCount + " for Client: '" + clientName + "'");

                    // ✅ Count ALL Interviews for this client (across ALL job IDs)
                    interviewCount += userDao.countAllInterviewsByClientNameAndDateRange(clientName, startDate, endDate);
                    System.out.println("Total Interview Count: " + interviewCount + " for Client: '" + clientName + "'");

                    // ✅ Count ALL Placements for this client (across ALL job IDs)
                    placementCount += userDao.countAllPlacementsByClientNameAndDateRange(clientName, startDate, endDate);
                    System.out.println("Total Placement Count: " + placementCount + " for Client: '" + clientName + "'");

                    // ✅ Count ALL Requirements for this client
                    requirementsCount += userDao.countRequirementsByClientNameAndDateRange(clientName, startDate, endDate);

                    System.out.println("Total Requirements Count: " + requirementsCount + " for Client: '" + clientName + "'");
                }
            }

            // Return DTO for BDM employee with all relevant counts
            return new BdmEmployeeDTO(
                    userId,
                    userName,
                    roleName,
                    user.getEmail(),
                    user.getStatus(),
                    clientCount,
                    requirementsCount,  // Moved requirementsCount after clientCount
                    submissionCount,  // Now submissionCount includes the total submissions for the BDM
                    interviewCount,
                    placementCount
            );
        }).collect(Collectors.toList());
    }

    public static UserDto convertEntityToDto(UserDetails user) {
        UserDto dto = new UserDto();

        dto.setUserId(user.getUserId());
        dto.setUserName(user.getUserName());
        dto.setPassword(user.getPassword());
        dto.setConfirmPassword(user.getConfirmPassword());
        dto.setEmail(user.getEmail());
        dto.setPersonalemail(user.getPersonalemail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setDob(user.getDob());
        dto.setFatherOrSpouseName(user.getFatherOrSpouseName());
        dto.setMotherName(user.getMotherName());
        dto.setBloodGroup(user.getBloodGroup());
        dto.setGender(user.getGender());
        dto.setMaritalStatus(user.getMaritalStatus());
        dto.setJoiningDate(user.getJoiningDate());
        dto.setDesignation(user.getDesignation());
        dto.setStatus(user.getStatus());
        dto.setEntity(user.getEntity());
        // Convert Set<Roles> to Set<UserType>
        Set<UserType> userTypes = user.getRoles()
                .stream()
                .map(Roles::getName) // assuming getName() returns UserType
                .collect(Collectors.toSet());

        dto.setRoles(userTypes);
        dto.setIsPrimarySuperAdmin(user.isPrimarySuperAdmin());
        dto.setTeamName(user.getTeamName());
        dto.setTeamAssignments(user.getTeamAssignments());
        dto.setPan(user.getPan());
        dto.setAdhar(user.getAdhar());
        dto.setCurrentAddress(user.getCurrentAddress());
        dto.setPermanentAddress(user.getPermanentAddress());
        dto.setEmergencyContactNo(user.getEmergencyContactNumber());
        dto.setEmergencyContactNumber(user.getEmergencyContactNumber());
        dto.setDoj(user.getDoj());
        dto.setOfficialNumber(user.getOfficialNumber());
        dto.setOfficialEmailId(user.getOfficialEmailId());
        dto.setProbation(user.getProbation());
        dto.setReportingManager(user.getReportingManager());
        dto.setDepartment(user.getDepartment());
        dto.setLinkedInUrl(user.getLinkedinUrl());
        dto.setLinkedinUrl(user.getLinkedinUrl());
        dto.setBankName(user.getBankName());
        dto.setAccountNumber(user.getAccountNumber());
        dto.setBranch(user.getBranch());
        dto.setAccountHolderName(user.getAccountHolderName());
        dto.setIfscCode(user.getIfscCode());
        dto.setIsEmployeeHavingPF(Boolean.TRUE.equals(user.getIsEmployeeHavingPF()));
        dto.setIsEmployeeHavingESI(Boolean.TRUE.equals(user.getIsEmployeeHavingESI()));
        dto.setEsiNumber(user.getEsiNumber());
        dto.setUanNumber(user.getUanNumber());
        dto.setPfNumber(user.getPfNumber());
        dto.setPayrollPanNumber(user.getPayrollPanNumber());
        dto.setPayrollAadharNumber(user.getPayrollAadharNumber());
        dto.setClearanceForm(user.getClearnessForm());
        dto.setClearnessForm(user.getClearnessForm());
        dto.setFAndF(user.getFAndF());
        dto.setExitFromPfDate(user.getExitFromPfDate());
        dto.setLastWorkingDay(user.getLastWorkingDay());
        dto.setIsEditable(Boolean.TRUE.equals(user.getIsEditable()));

        return dto;
    }

    public List<UserDto> getAllUsers() {
        List<UserDetails> users = userDao.findAll();

        List<UserDto> employees = users.stream()
                .map(UserService::convertEntityToDto)
                .collect(Collectors.toList());

        return employees;
    }

    public Page<UserDto> getAllFilteredUsers(String userId, String userName, String email, LocalDate joiningDate, Pageable pageable) {
        Specification<UserDetails> spec = Specification.where(null);

        if (userId != null && !userId.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("userId")), "%" + userId.toLowerCase() + "%"));
        }
        if (userName != null && !userName.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("userName")), "%" + userName.toLowerCase() + "%"));
        }
        if (email != null && !email.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }
        if (joiningDate != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("joiningDate"), joiningDate));
        }

        Page<UserDetails> userPage = userDao.findAll(spec, pageable);
        return userPage.map(this::convertToDtoUs);
    }

    public UserDto getUserByUserId(String userId) {

        UserDetails userDetails = userDao.findByUserId(userId);
        if (userDetails == null) {
            throw new NoSuchUserException("No User Found With ID :" + userId);
        }
        UserDto userDto = convertEntityToDto(userDetails);
        return userDto;
    }


    public UserDetailsDTO getUserByEmail(String email) {
        UserDetails user = userDao.findByEmail(email);
        if (user == null) {
            throw new NoSuchUserException("No User Found With Email ID : " + email);
        }
        return convertToDto(user);
    }

    public UserLoginStatusDTO getLoginStatusByUserId(String userId) {
        UserDetails user = userDao.findByUserId(userId);
        if (user == null) {
            throw new NoSuchUserException("No User Found With ID : " + userId);
        }
        UserLoginStatusDTO dto = new UserLoginStatusDTO();
        dto.setUserId(user.getUserId());
        dto.setLastLoginTime(user.getLastLoginTime());
        return dto;
    }

    private UserDetailsDTO convertToDto(UserDetails user) {
        UserDetailsDTO dto = new UserDetailsDTO();
        dto.setUserId(user.getUserId());
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setLastLoginTime(user.getLastLoginTime());
        return dto;
    }

    private UserDto convertToDtoUs(UserDetails user) {
        UserDto dto = new UserDto();

        dto.setUserId(user.getUserId());
        dto.setUserName(user.getUserName());
        dto.setPassword(user.getPassword());
        dto.setConfirmPassword(user.getConfirmPassword());
        dto.setEmail(user.getEmail());
        dto.setPersonalemail(user.getPersonalemail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setDob(user.getDob());
        dto.setFatherOrSpouseName(user.getFatherOrSpouseName());
        dto.setMotherName(user.getMotherName());
        dto.setBloodGroup(user.getBloodGroup());
        dto.setGender(user.getGender());
        dto.setMaritalStatus(user.getMaritalStatus());
        dto.setJoiningDate(user.getJoiningDate());
        dto.setDesignation(user.getDesignation());
        Set<UserType> collect = user.getRoles().stream().map(Roles::getName).collect(Collectors.toSet());
        dto.setRoles(collect);
        dto.setStatus(user.getStatus());
        dto.setEntity(user.getEntity());
        dto.setTeamName(user.getTeamName());
        dto.setTeamAssignments(user.getTeamAssignments());
        dto.setIsPrimarySuperAdmin(user.isPrimarySuperAdmin());
        dto.setPan(user.getPan());
        dto.setAdhar(user.getAdhar());
        dto.setCurrentAddress(user.getCurrentAddress());
        dto.setPermanentAddress(user.getPermanentAddress());
        dto.setEmergencyContactNo(user.getEmergencyContactNumber());
        dto.setEmergencyContactNumber(user.getEmergencyContactNumber());
        dto.setDoj(user.getDoj());
        dto.setOfficialNumber(user.getOfficialNumber());
        dto.setOfficialEmailId(user.getOfficialEmailId());
        dto.setProbation(user.getProbation());
        dto.setReportingManager(user.getReportingManager());
        dto.setDepartment(user.getDepartment());
        dto.setLinkedInUrl(user.getLinkedinUrl());
        dto.setLinkedinUrl(user.getLinkedinUrl());
        dto.setBankName(user.getBankName());
        dto.setAccountNumber(user.getAccountNumber());
        dto.setBranch(user.getBranch());
        dto.setAccountHolderName(user.getAccountHolderName());
        dto.setIfscCode(user.getIfscCode());
        dto.setIsEmployeeHavingPF(Boolean.TRUE.equals(user.getIsEmployeeHavingPF()));
        dto.setIsEmployeeHavingESI(Boolean.TRUE.equals(user.getIsEmployeeHavingESI()));
        dto.setEsiNumber(user.getEsiNumber());
        dto.setUanNumber(user.getUanNumber());
        dto.setPfNumber(user.getPfNumber());
        dto.setPayrollPanNumber(user.getPayrollPanNumber());
        dto.setPayrollAadharNumber(user.getPayrollAadharNumber());
        dto.setClearanceForm(user.getClearnessForm());
        dto.setClearnessForm(user.getClearnessForm());
        dto.setFAndF(user.getFAndF());
        dto.setExitFromPfDate(user.getExitFromPfDate());
        dto.setLastWorkingDay(user.getLastWorkingDay());
        dto.setIsEditable(Boolean.TRUE.equals(user.getIsEditable()));
        return dto;
    }


    public List<UserAssignment> getUserIdsAndUserNames(List<String> userIds) {

        List<UserDetails> users = userDao.findByUserIdIn(userIds);
        List<UserAssignment> userAssignments = users.stream()
                .map(userDetails -> {
                    UserAssignment userAssignment = new UserAssignment();
                    userAssignment.setUserId(userDetails.getUserId());
                    userAssignment.setUserName(userDetails.getUserName());
                    return userAssignment;
                }).collect(Collectors.toList());
        return userAssignments;
    }

    public List<UserAssignment> getUsersDropdown() {
        List<UserAssignment> usersDropDown = new ArrayList<>();
        userDao.findAll().stream()
                .filter(userDetails ->
                        userDetails.getRoles().stream().anyMatch(roles -> roles.getName() == UserType.SUPERADMIN) ||
                                userDetails.getEntity().equalsIgnoreCase("US"))
                .forEach(userDetails -> {
                    UserAssignment userAssignment = new UserAssignment();
                    userAssignment.setUserId(userDetails.getUserId());
                    userAssignment.setUserName(userDetails.getUserName());
                    usersDropDown.add(userAssignment);
                });

        return usersDropDown;
    }

    public Map<String, Object> getUserRoleAndUsername(String userId) {
        UserDetails user = userDao.findByUserId(userId);
        if (user == null) {
            throw new NoSuchUserException("No User Found With ID : " + userId);
        }

        // Extract roles as a list of names
        List<UserType> roleNames = user.getRoles().stream()
                .map(Roles::getName)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("userName", user.getUserName());
        return result;
    }

    public UserDetails getUserCreds(String userId) {
        UserDetails byUserId = userDao.findByUserId(userId);
        return byUserId;
    }

    public String setupAttendanceMonth(AttendanceMonthSetupDto dto) {

        try {

            AttendanceMonthConfig existingConfig =
                    attendanceMonthConfigRepository
                            .findByAttendanceMonthAndAttendanceYearAndEntity(
                                    dto.getMonth(),
                                    dto.getYear(),
                                    dto.getEntity())
                            .orElse(null);

            if (existingConfig != null) {
                throw new RuntimeException(
                        "Attendance month already configured for entity : "
                                + dto.getEntity());
            }

            LocalDate fromDate = LocalDate.of(dto.getYear(), dto.getMonth(), 1)
                    .minusMonths(1)
                    .withDayOfMonth(26);

            LocalDate toDate = LocalDate.of(dto.getYear(), dto.getMonth(), 25);

            AttendanceMonthConfig config = new AttendanceMonthConfig();
            config.setAttendanceMonth(dto.getMonth());
            config.setAttendanceYear(dto.getYear());
            config.setEntity(dto.getEntity());
            config.setFromDate(fromDate);
            config.setToDate(toDate);
            config.setPublicHolidays(dto.getPublicHolidays());
            config.setIsLocked(false);

            attendanceMonthConfigRepository.save(config);

            // Fetch all employees for selected entity
            List<UserDetails> employees =
                    userDao.findAllAttendanceEmployeesByEntity(dto.getEntity());

            List<EmployeeAttendance> saveList = new ArrayList<>();

            LocalDate currentDate = fromDate;

            while (!currentDate.isAfter(toDate)) {

                boolean isWeekend =
                        currentDate.getDayOfWeek() == DayOfWeek.SATURDAY
                                || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY;

                boolean isPublicHoliday =
                        dto.getPublicHolidays() != null
                                && dto.getPublicHolidays().contains(currentDate);

                Integer weekNumber = calculateWeekNumber(fromDate, currentDate);

                for (UserDetails employee : employees) {

                    EmployeeAttendance attendance = new EmployeeAttendance();

                    attendance.setEmployeeId(employee.getUserId());
                    attendance.setEmployeeName(employee.getUserName());
                    attendance.setAttendanceDate(currentDate);
                    attendance.setAttendanceMonth(dto.getMonth());
                    attendance.setAttendanceYear(dto.getYear());
                    attendance.setWeekNumber(weekNumber);
                    attendance.setMonthConfig(config);
                    attendance.setFromDate(fromDate);
                    attendance.setToDate(toDate);
                    attendance.setApprovalStatus("DRAFT");
                    attendance.setCreatedAt(LocalDateTime.now());
                    attendance.setUpdatedAt(LocalDateTime.now());
                    attendance.setIsLocked(false);
                    attendance.setSalaryDeduction(false);
                    attendance.setCasualLeaveApplied(false);
                    attendance.setIsSandwichDeduction(false);

                    if (employee.getAssociatedTeamLeadId() != null
                            && !employee.getAssociatedTeamLeadId().isBlank()) {

                        attendance.setReportingManager(
                                userDao.getTeamLeadName(
                                        employee.getAssociatedTeamLeadId()));
                    } else {
                        attendance.setReportingManager(
                                employee.getReportingManager());
                    }

                    boolean isProbation =
                            employee.getProbation() == null
                                    || !employee.getProbation().equalsIgnoreCase("Completed");

                    attendance.setIsProbationEmployee(isProbation);

                    if (isPublicHoliday) {

                        attendance.setAttendanceStatus("PH");
                        attendance.setAttendanceValue(1.0);
                        attendance.setIsPublicHoliday(true);
                        attendance.setIsWeekend(false);
                        attendance.setIsPaid(true);

                    } else if (isWeekend) {

                        attendance.setAttendanceStatus("WO");
                        attendance.setAttendanceValue(1.0);
                        attendance.setIsWeekend(true);
                        attendance.setIsPublicHoliday(false);
                        attendance.setIsPaid(true);

                    } else {

                        // Default Present
                        attendance.setAttendanceStatus("P");
                        attendance.setAttendanceValue(1.0);
                        attendance.setIsWeekend(false);
                        attendance.setIsPublicHoliday(false);
                        attendance.setIsPaid(true);
                    }

                    saveList.add(attendance);
                }

                currentDate = currentDate.plusDays(1);
            }

            attendanceRepository.saveAll(saveList);

            return "Attendance month configured successfully and default attendance generated.";

        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public String saveAttendance(AttendanceSaveRequestDto dto) {
        try {
            AttendanceMonthConfig monthConfig = attendanceMonthConfigRepository
                    .findByAttendanceMonthAndAttendanceYearAndEntity(
                            dto.getAttendanceMonth(),
                            dto.getAttendanceYear(),
                            dto.getEntity())
                    .orElseThrow(() ->
                            new RuntimeException("Attendance month not configured"));

            if (dto.getAttendanceDate().isBefore(monthConfig.getFromDate())
                    || dto.getAttendanceDate().isAfter(monthConfig.getToDate())) {

                throw new RuntimeException(
                        "Attendance date is outside configured cycle"
                );
            }

            Integer weekNumber;
            int day = dto.getAttendanceDate().getDayOfMonth();
            if (day >= 26 || day == 1) {
                weekNumber = 1;
            } else if (day >= 2 && day <= 8) {
                weekNumber = 2;
            } else if (day >= 9 && day <= 15) {
                weekNumber = 3;
            } else if (day >= 16 && day <= 22) {
                weekNumber = 4;
            } else {
                weekNumber = 5;
            }

            Long approvedCount = attendanceRepository.countApprovedWeek(
                    dto.getAttendanceMonth(),
                    dto.getAttendanceYear(),
                    weekNumber);
            if (approvedCount > 0) {
                throw new RuntimeException(
                        "This week is already approved. Attendance cannot be modified."
                );
            }

            boolean isWeekend =
                    dto.getAttendanceDate().getDayOfWeek() == DayOfWeek.SATURDAY
                            || dto.getAttendanceDate().getDayOfWeek() == DayOfWeek.SUNDAY;

            boolean isPublicHoliday =
                    monthConfig.getPublicHolidays() != null
                            && monthConfig.getPublicHolidays().contains(dto.getAttendanceDate());

            List<EmployeeAttendance> saveList = new ArrayList<>();
            for (EmployeeAttendanceDto employeeDto : dto.getEmployees()) {
                UserDetails employee = userDao.findByUserId(employeeDto.getEmployeeId());
                if (employee == null) {
                    continue;
                }

                EmployeeAttendance attendance = attendanceRepository
                        .findByEmployeeIdAndAttendanceDate(
                                employee.getUserId(),
                                dto.getAttendanceDate())
                        .orElse(null);

                if (attendance != null
                        && "APPROVED".equalsIgnoreCase(attendance.getApprovalStatus())) {

                    throw new RuntimeException(
                            "Attendance already approved for employee : "
                                    + employee.getUserId());
                }

                if(attendance!=null
                        &&
                        Boolean.TRUE.equals(
                                attendance.getIsLocked())){

                    throw new RuntimeException(
                            "Attendance is locked.");
                }

                if (attendance == null) {
                    attendance = new EmployeeAttendance();
                    attendance.setEmployeeId(employee.getUserId());
                    attendance.setEmployeeName(employee.getUserName());
                    attendance.setAttendanceDate(dto.getAttendanceDate());
                    attendance.setAttendanceMonth(dto.getAttendanceMonth());
                    attendance.setAttendanceYear(dto.getAttendanceYear());
                    attendance.setWeekNumber(weekNumber);
                    attendance.setMonthConfig(monthConfig);
                    attendance.setFromDate(monthConfig.getFromDate());
                    attendance.setToDate(monthConfig.getToDate());
                    attendance.setApprovalStatus("DRAFT");
                    attendance.setCreatedAt(LocalDateTime.now());
                }

                attendance.setIsLocked(false);

                if (employee.getAssociatedTeamLeadId() != null
                        && !employee.getAssociatedTeamLeadId().isBlank()) {

                    String teamLeadName =
                            userDao.getTeamLeadName(employee.getAssociatedTeamLeadId());

                    attendance.setReportingManager(teamLeadName);

                } else {

                    attendance.setReportingManager(employee.getReportingManager());
                }

                boolean isProbation =
                        employee.getProbation() == null
                                || !employee.getProbation().equalsIgnoreCase("Completed");

                attendance.setIsProbationEmployee(isProbation);

                if (isPublicHoliday) {
                    attendance.setAttendanceStatus("PH");
                    attendance.setAttendanceValue(1.0);
                    attendance.setIsPublicHoliday(true);
                    attendance.setIsWeekend(false);
                    attendance.setIsPaid(true);

                    // Allow editing
                    attendance.setIsLocked(false);

                } else if (isWeekend) {
                    attendance.setAttendanceStatus("WO");
                    attendance.setAttendanceValue(1.0);
                    attendance.setIsWeekend(true);
                    attendance.setIsPublicHoliday(false);
                    attendance.setIsPaid(true);

                    // Allow editing
                    attendance.setIsLocked(false);
                } else {
                    attendance.setAttendanceStatus(employeeDto.getAttendanceStatus());
                    attendance.setRemarks(employeeDto.getRemarks());

                    if ("HD".equalsIgnoreCase(employeeDto.getAttendanceStatus())) {

                        attendance.setAttendanceValue(
                                employeeDto.getAttendanceValue() != null
                                        ? employeeDto.getAttendanceValue()
                                        : 0.5);

                    } else {

                        attendance.setAttendanceValue(
                                employeeDto.getAttendanceValue() != null
                                        ? employeeDto.getAttendanceValue()
                                        : 1.0);
                    }
                    attendance.setIsWeekend(false);
                    attendance.setIsPublicHoliday(false);
                    attendance.setIsPaid(
                            !"LOP".equalsIgnoreCase(employeeDto.getAttendanceStatus()));

                    attendance.setIsLocked(false);
                }
                attendance.setSalaryDeduction(false);
                attendance.setCasualLeaveApplied(false);
                attendance.setIsSandwichDeduction(false);
                attendance.setUpdatedAt(LocalDateTime.now());
                saveList.add(attendance);
            }

            attendanceRepository.saveAll(saveList);
            return "Attendance saved successfully";

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<AttendanceDashboardResponseDto> getAttendanceDashboard(
            Integer month,
            Integer year,
            String entity) {

        try {

            List<AttendanceDashboardResponseDto> responseList =
                    new ArrayList<>();

            AttendanceMonthConfig monthConfig =
                    attendanceMonthConfigRepository
                            .findByAttendanceMonthAndAttendanceYearAndEntity(
                                    month,
                                    year,
                                    entity)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Attendance month not configured"));

            List<UserDetails> employees =
                    userDao.findAllAttendanceEmployeesByEntity(entity);

            int serialNo = 1;

            for (UserDetails employee : employees) {

                List<EmployeeAttendance> attendanceList =
                        attendanceRepository.getEmployeeAttendanceMonth(
                                employee.getUserId(),
                                month,
                                year);

                AttendanceDashboardResponseDto dto =
                        buildAttendanceDashboard(
                                employee,
                                attendanceList,
                                monthConfig,
                                serialNo++);

                responseList.add(dto);
            }

            return responseList;

        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<EmployeeAttendanceViewDto> getEmployeeAttendance(
            String employeeId,
            Integer month,
            Integer year,
            String entity) {

        AttendanceMonthConfig monthConfig = attendanceMonthConfigRepository.findByAttendanceMonthAndAttendanceYearAndEntity(month, year,entity)
                        .orElseThrow(() -> new RuntimeException("Attendance month not configured"));

        List<EmployeeAttendance> attendanceList = attendanceRepository.getEmployeeAttendanceMonth(employeeId, month, year);

        Map<LocalDate, EmployeeAttendance> attendanceMap = attendanceList.stream()
                        .collect(Collectors.toMap(
                                EmployeeAttendance::getAttendanceDate,
                                a -> a,
                                (a, b) -> a
                        ));

        List<EmployeeAttendanceViewDto> response = new ArrayList<>();

        LocalDate currentDate = monthConfig.getFromDate();

        while (!currentDate.isAfter(monthConfig.getToDate())) {

            EmployeeAttendance attendance = attendanceMap.get(currentDate);

            boolean isWeekend = currentDate.getDayOfWeek() == DayOfWeek.SATURDAY
                            ||
                            currentDate.getDayOfWeek() == DayOfWeek.SUNDAY;

            boolean isPublicHoliday = monthConfig.getPublicHolidays() != null
                            &&
                            monthConfig.getPublicHolidays()
                                    .contains(currentDate);

            EmployeeAttendanceViewDto dto = new EmployeeAttendanceViewDto();

            dto.setAttendanceDate(currentDate);
            dto.setIsWeekend(isWeekend);
            dto.setIsPublicHoliday(isPublicHoliday);

            if (isPublicHoliday) {

                dto.setAttendanceStatus("PH");
                dto.setAttendanceValue(1.0);

            } else if (isWeekend) {

                dto.setAttendanceStatus("WO");
                dto.setAttendanceValue(1.0);

            } else if (attendance != null) {

                dto.setAttendanceStatus(attendance.getAttendanceStatus());
                dto.setAttendanceValue(attendance.getAttendanceValue());
                dto.setRemarks(attendance.getRemarks());
                dto.setWeekNumber(attendance.getWeekNumber());
            }

            response.add(dto);

            currentDate = currentDate.plusDays(1);
        }

        return response;
    }

    public String editAttendance(AttendanceSaveRequestDto dto) {

        try {

            AttendanceMonthConfig monthConfig = attendanceMonthConfigRepository
                    .findByAttendanceMonthAndAttendanceYearAndEntity(dto.getAttendanceMonth(), dto.getAttendanceYear(), dto.getEntity())
                    .orElseThrow(() ->
                            new RuntimeException("Attendance month not configured"));

            if (dto.getAttendanceDate().isBefore(monthConfig.getFromDate())
                    || dto.getAttendanceDate().isAfter(monthConfig.getToDate())) {

                throw new RuntimeException(
                        "Attendance date is outside configured cycle");
            }

            List<EmployeeAttendance> updateList = new ArrayList<>();

            for (EmployeeAttendanceDto employeeDto : dto.getEmployees()) {

                EmployeeAttendance attendance = attendanceRepository
                        .findByEmployeeIdAndAttendanceDate(
                                employeeDto.getEmployeeId(),
                                dto.getAttendanceDate())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance not found for employee : "
                                                + employeeDto.getEmployeeId()));

                String status = attendance.getApprovalStatus();

                if (Boolean.TRUE.equals(attendance.getIsLocked())) {

                    throw new RuntimeException(
                            "Attendance is locked.");
                }
                attendance.setAttendanceStatus(employeeDto.getAttendanceStatus());
                attendance.setRemarks(employeeDto.getRemarks());

                if ("HD".equalsIgnoreCase(employeeDto.getAttendanceStatus())) {

                    attendance.setAttendanceValue(
                            employeeDto.getAttendanceValue() != null
                                    ? employeeDto.getAttendanceValue()
                                    : 0.5);

                } else {

                    attendance.setAttendanceValue(
                            employeeDto.getAttendanceValue() != null
                                    ? employeeDto.getAttendanceValue()
                                    : 1.0);
                }

                // Update flags based on edited status
                if ("WO".equalsIgnoreCase(employeeDto.getAttendanceStatus())) {

                    attendance.setIsWeekend(true);
                    attendance.setIsPublicHoliday(false);
                    attendance.setIsPaid(true);

                } else if ("PH".equalsIgnoreCase(employeeDto.getAttendanceStatus())) {

                    attendance.setIsWeekend(false);
                    attendance.setIsPublicHoliday(true);
                    attendance.setIsPaid(true);

                } else {

                    attendance.setIsWeekend(false);
                    attendance.setIsPublicHoliday(false);
                    attendance.setIsPaid(
                            !"LOP".equalsIgnoreCase(employeeDto.getAttendanceStatus()));
                }

                // Keep editable
                attendance.setIsLocked(false);
                attendance.setUpdatedAt(LocalDateTime.now());
                updateList.add(attendance);
            }

            attendanceRepository.saveAll(updateList);

            return "Attendance updated successfully";

        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
    public String editAttendanceMonth(AttendanceMonthSetupDto dto, String entity) {
        try {

            AttendanceMonthConfig config = attendanceMonthConfigRepository
                    .findByAttendanceMonthAndAttendanceYearAndEntity(dto.getMonth(), dto.getYear(), dto.getEntity())
                    .orElseThrow(() ->
                            new RuntimeException("Attendance month not configured"));

            LocalDate fromDate = LocalDate.of(dto.getYear(), dto.getMonth(), 1)
                    .minusMonths(1)
                    .withDayOfMonth(26);

            LocalDate toDate = LocalDate.of(dto.getYear(), dto.getMonth(), 25);

            config.setFromDate(fromDate);
            config.setToDate(toDate);
            config.setPublicHolidays(dto.getPublicHolidays());

            attendanceMonthConfigRepository.save(config);

            attendanceRepository.deleteWeekOffAndPublicHoliday(
                    dto.getMonth(),
                    dto.getYear());

            List<UserDetails> employees =
                    userDao.findAllAttendanceEmployeesByEntity(entity);

            List<EmployeeAttendance> saveList = new ArrayList<>();

            LocalDate currentDate = fromDate;
            while (!currentDate.isAfter(toDate)) {

                boolean isWeekend =
                        currentDate.getDayOfWeek() == DayOfWeek.SATURDAY
                                || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY;

                boolean isPublicHoliday =
                        dto.getPublicHolidays() != null
                                && dto.getPublicHolidays().contains(currentDate);

                if (!isWeekend && !isPublicHoliday) {
                    currentDate = currentDate.plusDays(1);
                    continue;
                }

                Integer weekNumber = calculateWeekNumber(fromDate, currentDate);

                for (UserDetails employee : employees) {
                    EmployeeAttendance attendance = new EmployeeAttendance();
                    attendance.setEmployeeId(employee.getUserId());
                    attendance.setEmployeeName(employee.getUserName());
                    attendance.setAttendanceDate(currentDate);
                    attendance.setAttendanceMonth(dto.getMonth());
                    attendance.setAttendanceYear(dto.getYear());
                    attendance.setWeekNumber(weekNumber);
                    attendance.setMonthConfig(config);
                    attendance.setFromDate(fromDate);
                    attendance.setToDate(toDate);
                    attendance.setApprovalStatus("DRAFT");
                    attendance.setAttendanceValue(1.0);

                    // Allow editing of WO/PH
                    attendance.setIsLocked(false);
                    attendance.setIsPaid(true);

                    if (employee.getAssociatedTeamLeadId() != null
                            && !employee.getAssociatedTeamLeadId().isBlank()) {

                        String teamLeadName =
                                userDao.getTeamLeadName(
                                        employee.getAssociatedTeamLeadId());

                        attendance.setReportingManager(teamLeadName);

                    } else {

                        attendance.setReportingManager(
                                employee.getReportingManager());
                    }

                    boolean probation =
                            employee.getProbation() == null
                                    || !employee.getProbation()
                                    .equalsIgnoreCase("Completed");

                    attendance.setIsProbationEmployee(probation);

                    if (isPublicHoliday) {
                        attendance.setAttendanceStatus("PH");
                        attendance.setIsPublicHoliday(true);
                        attendance.setIsWeekend(false);
                    } else {
                        attendance.setAttendanceStatus("WO");
                        attendance.setIsWeekend(true);
                        attendance.setIsPublicHoliday(false);
                    }
                    saveList.add(attendance);
                }

                currentDate = currentDate.plusDays(1);
            }

            attendanceRepository.saveAll(saveList);

            return "Attendance month updated successfully";

        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
    public String unlockRejectedWeek(
            Integer month,
            Integer year,
            Integer weekNumber,
            String entity) {

        List<EmployeeAttendance> attendanceList = attendanceRepository.findByMonthYearAndWeek(month, year, weekNumber,entity);

        if (attendanceList.isEmpty()) {
            throw new RuntimeException("No attendance found");
        }
        for (EmployeeAttendance attendance : attendanceList) {
            if (!"REJECTED".equalsIgnoreCase(attendance.getApprovalStatus())) {
                throw new RuntimeException("Only rejected weeks can be unlocked");
            }
            attendance.setApprovalStatus("DRAFT");
            attendance.setIsLocked(false);
        }
        attendanceRepository.saveAll(attendanceList);
        return "Week unlocked successfully";
    }
    public List<LocalDate> getAttendanceMonthHolidays(
            Integer month,
            Integer year,
            String entity) {

        try {

            AttendanceMonthConfig config = attendanceMonthConfigRepository
                    .findByAttendanceMonthAndAttendanceYearAndEntity(
                            month,
                            year,
                            entity)
                    .orElseThrow(() ->
                            new RuntimeException("Attendance month not configured"));

            return config.getPublicHolidays();

        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Transactional
    public String deleteAttendanceMonth(
            Integer month,
            Integer year,
            String entity) {

        try {

            AttendanceMonthConfig config = attendanceMonthConfigRepository.findByAttendanceMonthAndAttendanceYearAndEntity(
                                    month,
                                    year,
                                    entity)
                            .orElseThrow(() -> new RuntimeException("Attendance month configuration not found."));

            if (Boolean.TRUE.equals(config.getIsLocked())) {
                throw new RuntimeException(
                        "Attendance month is locked and cannot be deleted.");
            }

            // Delete all attendance records for this month configuration
            attendanceRepository.deleteByMonthConfig(config);

            // Delete the month configuration
            attendanceMonthConfigRepository.delete(config);

            return "Attendance month deleted successfully.";

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
    private List<EmployeeAttendance> getAttendanceForAction(
            AttendanceApprovalDto dto) {

        if (dto.getWeekNumber() != null) {

            return attendanceRepository.findByMonthYearAndWeek(
                    dto.getMonth(),
                    dto.getYear(),
                    dto.getWeekNumber(),
                    dto.getEntity());
        }

        return attendanceRepository.findMonthAttendance(
                dto.getMonth(),
                dto.getYear(),
                dto.getEntity());

    }

    private void validateAttendanceForSubmit(
            List<EmployeeAttendance> attendanceList,
            AttendanceApprovalDto dto) {

        if (attendanceList.isEmpty()) {
            throw new RuntimeException("Attendance not found.");
        }

        // WEEK SUBMISSION
        if (dto.getWeekNumber() != null) {

            for (EmployeeAttendance attendance : attendanceList) {

                if ("APPROVED".equalsIgnoreCase(attendance.getApprovalStatus())) {
                    throw new RuntimeException("Attendance is already approved.");
                }

                if ("SUBMITTED".equalsIgnoreCase(attendance.getApprovalStatus())) {
                    throw new RuntimeException("Attendance is already submitted.");
                }

                if (Boolean.TRUE.equals(attendance.getIsLocked())) {
                    throw new RuntimeException("Attendance is locked.");
                }
            }

            return;
        }

        // MONTH SUBMISSION

        boolean hasDraft = false;

        for (EmployeeAttendance attendance : attendanceList) {

            if ("APPROVED".equalsIgnoreCase(attendance.getApprovalStatus())) {
                throw new RuntimeException("Attendance is already approved.");
            }

            if ("DRAFT".equalsIgnoreCase(attendance.getApprovalStatus())) {
                hasDraft = true;
            }
        }

        if (hasDraft) {
            throw new RuntimeException(
                    "Please submit all weeks before submitting the month.");
        }
    }
    public String submitAttendance(
            AttendanceApprovalDto dto) {

        try {

            List<EmployeeAttendance> attendanceList =
                    getAttendanceForAction(dto);

            validateAttendanceForSubmit(attendanceList,dto);

            LocalDateTime submittedTime = LocalDateTime.now();

            for (EmployeeAttendance attendance : attendanceList) {

                attendance.setApprovalStatus("SUBMITTED");

                attendance.setSubmittedAt(submittedTime);

                attendance.setUpdatedAt(submittedTime);

                attendance.setUpdatedBy(dto.getActionBy());

                attendance.setIsLocked(true);
            }

            attendanceRepository.saveAll(attendanceList);

            if (dto.getWeekNumber() != null) {

                return "Week "
                        + dto.getWeekNumber()
                        + " submitted successfully.";

            }

            return "Month submitted successfully.";

        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
    public String approveAttendance(AttendanceApprovalDto dto) {

        try {

            List<EmployeeAttendance> attendanceList =
                    getAttendanceForAction(dto);

            if (attendanceList.isEmpty()) {
                throw new RuntimeException("Attendance not found.");
            }

            // ================= MONTH APPROVAL =================
            if (dto.getWeekNumber() == null) {

                AttendanceMonthConfig monthConfig =
                        attendanceMonthConfigRepository
                                .findByAttendanceMonthAndAttendanceYearAndEntity(
                                        dto.getMonth(),
                                        dto.getYear(),
                                        dto.getEntity())
                                .orElseThrow(() ->
                                        new RuntimeException("Attendance month configuration not found."));

                if (Boolean.TRUE.equals(monthConfig.getIsLocked())) {
                    throw new RuntimeException("Month attendance is already approved.");
                }

                // Check whether every attendance is approved
                boolean allApproved = attendanceList.stream()
                        .allMatch(a -> "APPROVED".equalsIgnoreCase(a.getApprovalStatus()));

                if (!allApproved) {
                    throw new RuntimeException(
                            "Please approve all weeks before approving the month.");
                }

                monthConfig.setIsLocked(true);
                attendanceMonthConfigRepository.save(monthConfig);

                return "Month approved successfully.";
            }

            // ================= WEEK APPROVAL =================
            for (EmployeeAttendance attendance : attendanceList) {

                if ("APPROVED".equalsIgnoreCase(attendance.getApprovalStatus())) {
                    throw new RuntimeException("Attendance is already approved.");
                }

                if (!"SUBMITTED".equalsIgnoreCase(attendance.getApprovalStatus())) {
                    throw new RuntimeException("Only submitted attendance can be approved.");
                }

                attendance.setApprovalStatus("APPROVED");
                attendance.setApprovedAt(LocalDateTime.now());
                attendance.setUpdatedAt(LocalDateTime.now());
                attendance.setUpdatedBy(dto.getActionBy());
                attendance.setIsLocked(true);
            }

            attendanceRepository.saveAll(attendanceList);

            return "Week " + dto.getWeekNumber() + " approved successfully.";

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }    public String rejectAttendance(
            AttendanceApprovalDto dto){

        List<EmployeeAttendance> attendanceList =
                getAttendanceForAction(dto);

        if(attendanceList.isEmpty()){
            throw new RuntimeException(
                    "Attendance not found");
        }

        for(EmployeeAttendance attendance:attendanceList){

            if(!"SUBMITTED".equalsIgnoreCase(
                    attendance.getApprovalStatus())){

                throw new RuntimeException(
                        "Only submitted attendance can be rejected.");
            }

            attendance.setApprovalStatus("REJECTED");

            attendance.setUpdatedAt(
                    LocalDateTime.now());

            attendance.setUpdatedBy(
                    dto.getActionBy());

            attendance.setIsLocked(false);
        }

        attendanceRepository.saveAll(attendanceList);

        return dto.getWeekNumber()==null
                ? "Month rejected successfully."
                : "Week rejected successfully.";
    }

    private AttendanceDashboardResponseDto buildAttendanceDashboard(
            UserDetails employee,
            List<EmployeeAttendance> attendanceList,
            AttendanceMonthConfig monthConfig,
            Integer serialNo) {

        AttendanceDashboardResponseDto dto =
                new AttendanceDashboardResponseDto();

        Map<String, String> attendanceGrid = new LinkedHashMap<>();

        double totalPresentDays = 0.0;
        int totalLeaves = 0;
        int totalLopLeaves = 0;
        int casualLeaves = 0;
        double totalPaidDays = 0.0;
        int totalWeekendDays = 0;
        int totalWorkingDays = 0;
        double halfDayDeduction = 0.0;   // NEW

        dto.setSerialNo(serialNo);
        dto.setEmployeeId(employee.getUserId());
        dto.setEmployeeName(employee.getUserName());
        dto.setDesignation(employee.getDesignation());
        dto.setJoiningDate(employee.getJoiningDate());
        dto.setProbation(employee.getProbation());

        dto.setPf(Boolean.TRUE.equals(employee.getIsEmployeeHavingPF())
                ? "YES"
                : "NO");

        dto.setEsi(Boolean.TRUE.equals(employee.getIsEmployeeHavingESI())
                ? "YES"
                : "NO");

        if (employee.getAssociatedTeamLeadId() != null
                && !employee.getAssociatedTeamLeadId().isBlank()) {

            dto.setReportingManager(
                    userDao.getTeamLeadName(
                            employee.getAssociatedTeamLeadId()));

        } else {

            dto.setReportingManager(
                    employee.getReportingManager());
        }

        Map<LocalDate, EmployeeAttendance> attendanceMap =
                attendanceList.stream()
                        .collect(Collectors.toMap(
                                EmployeeAttendance::getAttendanceDate,
                                a -> a,
                                (a, b) -> a));

        LocalDate currentDate = monthConfig.getFromDate();

        while (!currentDate.isAfter(monthConfig.getToDate())) {

            int day = currentDate.getDayOfMonth();

            EmployeeAttendance attendance =
                    attendanceMap.get(currentDate);

            boolean isWeekend =
                    currentDate.getDayOfWeek() == DayOfWeek.SATURDAY
                            || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY;

            boolean isPublicHoliday =
                    monthConfig.getPublicHolidays() != null
                            && monthConfig.getPublicHolidays().contains(currentDate);

            if (isWeekend) {
                totalWeekendDays++;
            }

            if (!isWeekend && !isPublicHoliday) {
                totalWorkingDays++;
            }

            String attendanceStatus = "";

            if (attendance != null
                    && attendance.getAttendanceStatus() != null) {

                attendanceStatus =
                        attendance.getAttendanceStatus();

            } else if (isPublicHoliday) {

                attendanceStatus = "PH";

            } else if (isWeekend) {

                attendanceStatus = "WO";
            }

            attendanceGrid.put(
                    String.valueOf(day),
                    attendanceStatus);

            // ===== Modified Present Calculation =====
            if ("HD".equalsIgnoreCase(attendanceStatus)) {

                totalPresentDays += 0.5;
                halfDayDeduction += 0.5;

            } else if ("P".equalsIgnoreCase(attendanceStatus)
                    || "WH".equalsIgnoreCase(attendanceStatus)
                    || "WFH".equalsIgnoreCase(attendanceStatus)
                    || "LL".equalsIgnoreCase(attendanceStatus)
                    || "SP".equalsIgnoreCase(attendanceStatus)) {

                totalPresentDays += 1;
            }

            if ("L".equalsIgnoreCase(attendanceStatus)) {
                totalLeaves++;
            }

            if ("LOP".equalsIgnoreCase(attendanceStatus)) {
                totalLopLeaves++;
            }

            currentDate = currentDate.plusDays(1);
        }

        boolean isProbationEmployee =
                employee.getProbation() == null
                        || !employee.getProbation()
                        .equalsIgnoreCase("Completed");

        int allowedCasualLeave =
                isProbationEmployee ? 0 : 1;

        Set<LocalDate> sandwichDeductionDates = new HashSet<>();

        Set<LocalDate> leaveDates = attendanceList.stream()
                .filter(a -> "L".equalsIgnoreCase(a.getAttendanceStatus()))
                .map(EmployeeAttendance::getAttendanceDate)
                .collect(Collectors.toSet());

        for (LocalDate leaveDate : leaveDates) {

            if (leaveDate.getDayOfWeek() == DayOfWeek.FRIDAY) {

                LocalDate saturday = leaveDate.plusDays(1);
                LocalDate sunday = leaveDate.plusDays(2);

                if (!leaveDates.contains(saturday)) {
                    sandwichDeductionDates.add(saturday);
                }

                if (!leaveDates.contains(sunday)) {
                    sandwichDeductionDates.add(sunday);
                }
            }

            if (leaveDate.getDayOfWeek() == DayOfWeek.MONDAY) {

                LocalDate saturday = leaveDate.minusDays(2);
                LocalDate sunday = leaveDate.minusDays(1);

                if (!leaveDates.contains(saturday)) {
                    sandwichDeductionDates.add(saturday);
                }

                if (!leaveDates.contains(sunday)) {
                    sandwichDeductionDates.add(sunday);
                }
            }
        }

        int unpaidLeaves = 0;

        if (totalLeaves <= allowedCasualLeave) {

            casualLeaves = totalLeaves;

        } else {

            casualLeaves = allowedCasualLeave;
            unpaidLeaves = totalLeaves - allowedCasualLeave;
        }

        totalPaidDays = totalPresentDays;

        LocalDate date = monthConfig.getFromDate();

        while (!date.isAfter(monthConfig.getToDate())) {

            boolean isPH =
                    monthConfig.getPublicHolidays() != null
                            && monthConfig.getPublicHolidays().contains(date);

            boolean isWO =
                    date.getDayOfWeek() == DayOfWeek.SATURDAY
                            || date.getDayOfWeek() == DayOfWeek.SUNDAY;

            if (isPH || isWO) {
                totalPaidDays += 1;
            }

            date = date.plusDays(1);
        }

        totalPaidDays += casualLeaves;
        totalPaidDays -= unpaidLeaves;
        totalPaidDays -= totalLopLeaves;
        totalPaidDays -= halfDayDeduction;
        totalPaidDays -= sandwichDeductionDates.size();

        if (totalPaidDays < 0) {
            totalPaidDays = 0;
        }

        int totalDaysInMonth =
                (int) ChronoUnit.DAYS.between(
                        monthConfig.getFromDate(),
                        monthConfig.getToDate()) + 1;

        dto.setAttendanceGrid(attendanceGrid);
        dto.setTotalDaysInMonth(totalDaysInMonth);
        dto.setTotalWorkingDays(totalWorkingDays);
        dto.setTotalWeekendDays(totalWeekendDays);
        dto.setTotalPresentDays(totalPresentDays);
        dto.setTotalLeaves(totalLeaves);
        dto.setCasualLeaves(casualLeaves);
        dto.setTotalPaidDays(totalPaidDays);

        return dto;
    }
    public List<AttendanceDashboardResponseDto> getPendingAttendance(
            Integer month,
            Integer year,
            Integer weekNumber,
            String entity) {

        List<AttendanceDashboardResponseDto> responseList =
                new ArrayList<>();

        AttendanceMonthConfig monthConfig =
                attendanceMonthConfigRepository
                        .findByAttendanceMonthAndAttendanceYearAndEntity(
                                month,
                                year,
                                entity)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance month not configured"));

        List<UserDetails> employees =
                userDao.findAllAttendanceEmployeesByEntity(entity);

        int serialNo = 1;

        for (UserDetails employee : employees) {

            List<EmployeeAttendance> attendanceList;

            if (weekNumber != null) {

                attendanceList =
                        attendanceRepository
                                .findSubmittedWeekAttendance(
                                        month,
                                        year,
                                        weekNumber,entity)
                                .stream()
                                .filter(a ->
                                        a.getEmployeeId()
                                                .equals(employee.getUserId()))
                                .toList();

            } else {

                attendanceList =
                        attendanceRepository
                                .findSubmittedMonthAttendance(
                                        month,
                                        year,entity)
                                .stream()
                                .filter(a ->
                                        a.getEmployeeId()
                                                .equals(employee.getUserId()))
                                .toList();
            }

            if(attendanceList.isEmpty()){
                continue;
            }

            AttendanceDashboardResponseDto dto =
                    buildAttendanceDashboard(
                            employee,
                            attendanceList,
                            monthConfig,
                            serialNo++);

            responseList.add(dto);
        }

        return responseList;
    }

    @Transactional
    public String deleteAttendanceMonthconfig(
            Integer month,
            Integer year,
            String entity) {

        AttendanceMonthConfig config =
                attendanceMonthConfigRepository
                        .findByAttendanceMonthAndAttendanceYearAndEntity(
                                month,
                                year,
                                entity)
                        .orElseThrow(() ->
                                new RuntimeException("Attendance month configuration not found."));

        if (Boolean.TRUE.equals(config.getIsLocked())) {
            throw new RuntimeException("Attendance month is locked and cannot be deleted.");
        }

        // Delete attendance first
        attendanceRepository.deleteAttendanceByMonthconfig(month, year, entity);

        // Delete month configuration
        attendanceMonthConfigRepository.delete(config);

        return "Attendance month deleted successfully.";
    }

    private Integer calculateWeekNumber(LocalDate fromDate, LocalDate currentDate) {

        int weekNumber = 1;
        LocalDate tempDate = fromDate;

        while (tempDate.isBefore(currentDate)) {

            if (tempDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                weekNumber++;
            }

            tempDate = tempDate.plusDays(1);
        }

        return weekNumber;
    }
}




