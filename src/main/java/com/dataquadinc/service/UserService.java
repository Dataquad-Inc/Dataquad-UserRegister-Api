
package com.dataquadinc.service;

import com.dataquadinc.dto.*;
import com.dataquadinc.exceptions.DateRangeValidationException;
import com.dataquadinc.exceptions.NoSuchUserException;
import com.dataquadinc.exceptions.UserNotFoundException;
import com.dataquadinc.exceptions.ValidationException;
import com.dataquadinc.mapper.UserMapper;
import com.dataquadinc.model.Roles;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserType;
import com.dataquadinc.repository.RolesDao;
import com.dataquadinc.repository.UserDao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

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

//    public ResponseEntity<ResponseBean<UserResponse>> updateUser(String userId, UserDto userDto) {
//        Map<String, String> errors = new HashMap<>();
//
//        // Check if the user exists
//        UserDetails existingUser = userDao.findByUserId(userId);
//        if (existingUser == null) {
//            ResponseBean<UserResponse> resp = new ResponseBean<>();
//            resp.setSuccess(false);
//            resp.setMessage("User not found");
//            return new ResponseEntity<>(resp, HttpStatus.NOT_FOUND);
//        }
//
//        if (userDto.getUserName() == null || userDto.getUserName().isEmpty()) {
//            errors.put("userName", "User name is required and cannot be null or empty");
//            ResponseBean<UserResponse> resp = new ResponseBean<>();
//            resp.setSuccess(false);
//            resp.setMessage("Validation failed");
//            return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
//
//
//            // Check if personalemail is provided and valid
//            if (userDto.getPersonalemail() == null || userDto.getPersonalemail().isEmpty()) {
//                errors.put("personalemail", "Personal email is required and cannot be null or empty");
//                ResponseBean<UserResponse> resp = new ResponseBean<>();
//                resp.setSuccess(false);
//                resp.setMessage("Validation failed");
//                return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
//            }
//
//        // Update the details (for example, the email, user name, etc.)
//        existingUser.setUserName(userDto.getUserName());
//        existingUser.setEmail(userDto.getEmail());
//        existingUser.setStatus(userDto.getStatus());
//        existingUser.setGender(userDto.getGender());
//        existingUser.setDesignation(userDto.getDesignation());
//        existingUser.setDob(userDto.getDob());
//        existingUser.setPersonalemail(userDto.getPersonalemail());  // Ensure this is not null or empty
//        existingUser.setJoiningDate(userDto.getJoiningDate());
//        existingUser.setPhoneNumber(userDto.getPhoneNumber());
//
//        // If password is provided, encode it and update it
//        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
//            existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
//        }
//
//        // Handle roles update
//        Set<Roles> roles = userDto.getRoles().stream()
//                .map(role -> {
//                    try {
//                        return rolesDao.findByName(role)
//                                .orElseThrow(() -> new ValidationException(Map.of("role", "roleNotFound")));
//                    } catch (ValidationException e) {
//                        throw new RuntimeException(e);
//                    }
//                })
//                .collect(Collectors.toSet());
//        existingUser.setRoles(roles);
//
//        // Save the updated user
//        UserDetails updatedUser = userDao.save(existingUser);
//        System.out.println("Saved UserName: " + updatedUser.getUserName());
//
//        // Prepare response
//        UserResponse userResponse = new UserResponse();
//        userResponse.setUserName(updatedUser.getUserName());
//        userResponse.setUserId(updatedUser.getUserId());
//        userResponse.setEmail(updatedUser.getEmail());
//
//        // Prepare the response bean
//        ResponseBean<UserResponse> responseBean = new ResponseBean<>();
//        responseBean.setSuccess(true);
//        responseBean.setMessage("User updated successfully");
//        responseBean.setData(userResponse);
//        responseBean.setError(null);
//
//        return new ResponseEntity<>(responseBean, HttpStatus.OK);
//    }


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

        return dto;
    }

    public List<UserDto> getAllUsers(){
      List<UserDetails> users=userDao.findAll();

        List<UserDto> employees =users.stream()
             .map(UserService::convertEntityToDto)
             .collect(Collectors.toList());

     return employees;
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
}


















