
package com.dataquadinc.service;

import com.dataquadinc.dto.*;
import com.dataquadinc.exceptions.DateRangeValidationException;
import com.dataquadinc.exceptions.NoSuchUserException;
import com.dataquadinc.exceptions.UserNotFoundException;
import com.dataquadinc.exceptions.ValidationException;
import com.dataquadinc.mapper.UserMapper;
import com.dataquadinc.model.Roles;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserProfileDocument;
import com.dataquadinc.model.UserType;
import com.dataquadinc.repository.RolesDao;
import com.dataquadinc.repository.UserDao;
import com.dataquadinc.repository.UserProfileDocumentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    public ResponseEntity<ResponseBean<UserResponse>> registerUser(UserDto userDto)  {
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
                .allMatch(r ->UserType.EMPLOYEE.name().equalsIgnoreCase(r.getName().name()));

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
            String userId, String roleName, String excludeRoleName) {

        UserType includeRole = null;
        UserType excludeRole = null;

        // Parse roleName
        if (roleName != null && !roleName.isBlank()) {
            try {
                includeRole = UserType.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid role name provided: {}", roleName);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        // Parse excludeRoleName
        if (excludeRoleName != null && !excludeRoleName.isBlank()) {
            try {
                excludeRole = UserType.valueOf(excludeRoleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid exclude role name provided: {}", excludeRoleName);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        List<UserDetails> users;

        if (excludeRole != null) {
            // ✅ Proper "NOT EXISTS" exclusion, also applies entity='IN'
            users = userDao.findByUserIdAndRoleNot(userId, excludeRole);
            logger.info("Fetched {} users with userId={} excluding role={}, entity='IN'",
                    users.size(), userId, excludeRole);

        } else if (includeRole != null) {
            // ✅ Include specific role, entity='IN'
            users = userDao.findByUserIdAndRole(userId, includeRole);
            logger.info("Fetched {} users with userId={} and role={}, entity='IN'",
                    users.size(), userId, includeRole);

        } else {
            // ✅ No role filter, still entity='IN'
            users = (userId == null || userId.isBlank())
                    ? userDao.findAllActiveNonTestUsers()
                    : userDao.findByUserIdAndRole(userId, null);

            logger.info("Fetched {} users with userId={}, no role filter, entity='IN'",
                    users.size(), userId);
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

                    return new EmployeeWithRole(
                            user.getUserId(),
                            user.getUserName(),
                            rolesString,
                            user.getEmail(),
                            user.getDesignation(),
                            user.getJoiningDate(),
                            user.getGender(),
                            user.getDob(),
                            user.getPhoneNumber(),
                            user.getPersonalemail(),
                            user.getStatus()
                    );
                })
                .collect(Collectors.toList());

        logger.info("Returning {} employee records in response", employeeRoles.size());
        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    public ResponseEntity<List<EmployeeWithRole>> findAllActiveInternal() {

        List<UserDetails> users=userDao.findAllActiveNotExternalUser();

        List<EmployeeWithRole> employeeRoles = users.stream()
                .map(user -> {
                    String rolesString = user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.joining(", "));

                    return new EmployeeWithRole(
                            user.getUserId(),
                            user.getUserName(),
                            rolesString,
                            user.getEmail(),
                            user.getDesignation(),
                            user.getJoiningDate(),
                            user.getGender(),
                            user.getDob(),
                            user.getPhoneNumber(),
                            user.getPersonalemail(),
                            user.getStatus()
                    );
                })
                .collect(Collectors.toList());

        logger.info("Returning {} employee records in response", employeeRoles.size());
        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    public ResponseEntity<List<EmployeeWithRole>> findAllActiveExternal() {

        List<UserDetails> users=userDao.findAllActiveExternalUser();

        List<EmployeeWithRole> employeeRoles = users.stream()
                .map(user -> {
                    String rolesString = user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.joining(", "));

                    return new EmployeeWithRole(
                            user.getUserId(),
                            user.getUserName(),
                            rolesString,
                            user.getEmail(),
                            user.getDesignation(),
                            user.getJoiningDate(),
                            user.getGender(),
                            user.getDob(),
                            user.getPhoneNumber(),
                            user.getPersonalemail(),
                            user.getStatus()
                    );
                })
                .collect(Collectors.toList());

        logger.info("Returning {} employee records in response", employeeRoles.size());
        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    public ResponseEntity<List<EmployeeWithRole>> findAllInActiveInternal() {

        List<UserDetails> users=userDao.findAllInActiveNotExternalUser();

        List<EmployeeWithRole> employeeRoles = users.stream()
                .map(user -> {
                    String rolesString = user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.joining(", "));

                    return new EmployeeWithRole(
                            user.getUserId(),
                            user.getUserName(),
                            rolesString,
                            user.getEmail(),
                            user.getDesignation(),
                            user.getJoiningDate(),
                            user.getGender(),
                            user.getDob(),
                            user.getPhoneNumber(),
                            user.getPersonalemail(),
                            user.getStatus()
                    );
                })
                .collect(Collectors.toList());

        logger.info("Returning {} employee records in response", employeeRoles.size());
        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    public ResponseEntity<List<EmployeeWithRole>> findAllInActiveExternal() {

        List<UserDetails> users=userDao.findAllInActiveExternalUser();

        List<EmployeeWithRole> employeeRoles = users.stream()
                .map(user -> {
                    String rolesString = user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.joining(", "));

                    return new EmployeeWithRole(
                            user.getUserId(),
                            user.getUserName(),
                            rolesString,
                            user.getEmail(),
                            user.getDesignation(),
                            user.getJoiningDate(),
                            user.getGender(),
                            user.getDob(),
                            user.getPhoneNumber(),
                            user.getPersonalemail(),
                            user.getStatus()
                    );
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

            String joiningDate = submittedValue(formFields, "joiningDate");
            if (joiningDate != null) {
                existingUser.setJoiningDate(LocalDate.parse(joiningDate));
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
                        .map(value -> rolesDao.findByName(UserType.valueOf(value.toUpperCase()))
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
            long clientCount = userDao.countClientsByUserIdAndDateRange(userId,startDate,endDate);
            System.out.println("Client Count: " + clientCount);

            // ✅ Get client names for this BDM
            List<String> clientNames = userDao.findClientNamesByUserIdAndDateRange(userId,startDate,endDate);
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
                    submissionCount += userDao.countAllSubmissionsByClientNameAndDateRange(clientName,startDate,endDate); // Updated method for count
                    System.out.println("Total Submission Count: " + submissionCount + " for Client: '" + clientName + "'");

                    // ✅ Count ALL Interviews for this client (across ALL job IDs)
                    interviewCount += userDao.countAllInterviewsByClientNameAndDateRange(clientName,startDate,endDate);
                    System.out.println("Total Interview Count: " + interviewCount + " for Client: '" + clientName + "'");

                    // ✅ Count ALL Placements for this client (across ALL job IDs)
                    placementCount += userDao.countAllPlacementsByClientNameAndDateRange(clientName,startDate,endDate);
                    System.out.println("Total Placement Count: " + placementCount + " for Client: '" + clientName + "'");

                    // ✅ Count ALL Requirements for this client
                    requirementsCount += userDao.countRequirementsByClientNameAndDateRange(clientName,startDate,endDate);
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

            return new EmployeeWithRole(
                    user.getUserId(),
                    user.getUserName(),
                    roleName,
                    user.getEmail(),
                    user.getDesignation(),
                    user.getJoiningDate(),
                    user.getGender(),
                    user.getDob(),
                    user.getPhoneNumber(),
                    user.getPersonalemail(),
                    user.getStatus());
        }).collect(Collectors.toList());
    }

    public List<BdmEmployeeDTO> getAllBdmEmployeesDateFilter(LocalDate startDate,LocalDate endDate) {
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
            long clientCount = userDao.countClientsByUserIdAndDateRange(userId,startDate,endDate);
            System.out.println("Client Count: " + clientCount);

            // ✅ Get client names for this BDM
            List<String> clientNames = userDao.findClientNamesByUserIdAndDateRange(userId,startDate,endDate);
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
                    submissionCount += userDao.countAllSubmissionsByClientNameAndDateRange(clientName,startDate,endDate); // Updated method for count
                    System.out.println("Total Submission Count: " + submissionCount + " for Client: '" + clientName + "'");

                    // ✅ Count ALL Interviews for this client (across ALL job IDs)
                    interviewCount += userDao.countAllInterviewsByClientNameAndDateRange(clientName,startDate,endDate);
                    System.out.println("Total Interview Count: " + interviewCount + " for Client: '" + clientName + "'");

                    // ✅ Count ALL Placements for this client (across ALL job IDs)
                    placementCount += userDao.countAllPlacementsByClientNameAndDateRange(clientName,startDate,endDate);
                    System.out.println("Total Placement Count: " + placementCount + " for Client: '" + clientName + "'");

                    // ✅ Count ALL Requirements for this client
                    requirementsCount += userDao.countRequirementsByClientNameAndDateRange(clientName,startDate,endDate);

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
        dto.setGender(user.getGender());
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

        return dto;
    }

    public List<UserDto> getAllUsers(){
      List<UserDetails> users=userDao.findAll();

        List<UserDto> employees =users.stream()
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

    public UserDto getUserByUserId(String userId){

        UserDetails userDetails=userDao.findByUserId(userId);
        if(userDetails==null){
            throw new NoSuchUserException("No User Found With ID :"+userId);
        }
        UserDto userDto=convertEntityToDto(userDetails);
        return userDto;
    }


    public UserDetailsDTO getUserByEmail(String email) {
        UserDetails user = userDao.findByEmail(email);
        if(user==null){
            throw new NoSuchUserException("No User Found With Email ID : " + email);
        }        return convertToDto(user);
    }

    public UserLoginStatusDTO getLoginStatusByUserId(String userId) {
        UserDetails user = userDao.findByUserId(userId);
        if(user==null){
            throw new NoSuchUserException("No User Found With ID : "+ userId);
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
        dto.setGender(user.getGender());
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
        return dto;
    }


    public List<UserAssignment> getUserIdsAndUserNames(List<String> userIds){

        List<UserDetails> users=userDao.findByUserIdIn(userIds);
        List<UserAssignment> userAssignments=users.stream()
                .map(userDetails -> {
                    UserAssignment userAssignment=new UserAssignment();
                    userAssignment.setUserId(userDetails.getUserId());
                    userAssignment.setUserName(userDetails.getUserName());
                    return userAssignment;
                }).collect(Collectors.toList());
        return userAssignments;
    }

    public List<UserAssignment> getUsersDropdown(){
        List<UserAssignment> usersDropDown=new ArrayList<>();
        userDao.findAll().stream()
                .filter(userDetails ->
                        userDetails.getRoles().stream().anyMatch(roles -> roles.getName()==UserType.SUPERADMIN) ||
                                userDetails.getEntity().equalsIgnoreCase("US"))
                .forEach(userDetails ->{
                    UserAssignment userAssignment=new UserAssignment();
                    userAssignment.setUserId(userDetails.getUserId());
                    userAssignment.setUserName(userDetails.getUserName());
                    usersDropDown.add(userAssignment);
                });

        return usersDropDown;
    }

    public Map<String, Object> getUserRoleAndUsername(String userId) {
        UserDetails user = userDao.findByUserId(userId);
                if(user == null) {
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
}


















