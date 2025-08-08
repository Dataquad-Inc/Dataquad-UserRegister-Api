
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

import org.apache.catalina.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserDao userDao;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RolesDao rolesDao;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


//    public ResponseEntity<ResponseBean<UserResponse>> registerUser(UserDto userDto) throws ValidationException {
//
//        Map<String,String> errors = new HashMap<>();
//
//
////        if (userDao.findByUserName(userDto.getUserName()) != null) {
////
////            errors.put("userName","userName already exists");
////        }
//
//        if (userDao.findByEmail(userDto.getEmail())!=null) {
//            errors.put("errormessage","email is already in use");
//        }
//        if (userDao.findByUserId(userDto.getUserId())!=null) {
//            errors.put("errorMessage","userId already exists");
//        }
//
//        if( !errors.isEmpty()) {
//            throw new ValidationException(errors);
//        }
//
//
//        // Encrypt the password
//        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
//        userDto.setConfirmPassword(passwordEncoder.encode(userDto.getConfirmPassword()));
//
//        // Convert DTO to entity
//        UserDetails user = userMapper.toEntity(userDto);
//
//        Set<Roles> roles = userDto.getRoles().stream()
//                .map(role -> {
//                    try {
//                        return rolesDao.findByName(role) // Find Role by its name from RolesDao
//                                .orElseThrow(() -> new ValidationException(Map.of("role","roleNotFound")));
//                    } catch (ValidationException e) {
//                        throw new RuntimeException(e);
//                    }
//                })
//                .collect(Collectors.toSet());
//
//        user.setRoles(roles);
//
//        // Save the user to the database
//        UserDetails dbUser = userDao.save(user);
//
//        UserResponse res=new UserResponse();
//        res.setUserName(dbUser.getUserName());
//        res.setUserId(dbUser.getUserId());
//        res.setEmail(dbUser.getEmail());
//        // Set success to true
//
//
//        ResponseBean<UserResponse> resp = new ResponseBean<UserResponse>();
//        resp.setSuccess(true);
//        resp.setMessage(" Created Sucessfully");
//        resp.setData(res);
//        resp.setError(null);
//
//        return new ResponseEntity<ResponseBean<UserResponse>>(resp,HttpStatus.CREATED);
//
//    }

    public ResponseEntity<ResponseBean<UserResponse>> registerUser(UserDto userDto)  {
        Map<String, String> errors = new HashMap<>();

        logger.info("New User Registering ...{}",userDto.getUserId());
        // Check if email or userId already exists
        if (userDao.findByEmail(userDto.getEmail()) != null) {
            errors.put("errormessage", userDto.getEmail()+" is already in use");
        }
        if (userDao.findByUserId(userDto.getUserId()) != null) {
            errors.put("errorMessage", userDto.getUserId() + " already exists. Please log in");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // Encrypt the password
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        userDto.setConfirmPassword(passwordEncoder.encode(userDto.getConfirmPassword()));

        // Convert DTO to entity
        UserDetails user = userMapper.toEntity(userDto);
        user.setEncryptionKey("MyMulya@1234");
        user.setPrimarySuperAdmin(false);

        // Map roles to the user
        Set<Roles> roles = userDto.getRoles().stream()
                .map(role -> {
                    try {
                        return rolesDao.findByName(role) // Find Role by its name from RolesDao
                                .orElseThrow(() -> new ValidationException(Map.of("role", "roleNotFound")));
                    } catch (ValidationException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());

        user.setRoles(roles);

        // Save the user to the database
        UserDetails dbUser = userDao.save(user);

        // Create a response object
        UserResponse res = new UserResponse();
        res.setUserName(dbUser.getUserName());
        res.setUserId(dbUser.getUserId());
        res.setEmail(dbUser.getEmail());

        // Create and send registration success email
        sendRegistrationConfirmationEmail(dbUser.getEmail());

        // Prepare the response bean
        ResponseBean<UserResponse> resp = new ResponseBean<>();
        resp.setSuccess(true);
        resp.setMessage("Created Successfully");
        resp.setData(res);
        resp.setError(null);

        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // Method to send the registration confirmation email
    private void sendRegistrationConfirmationEmail(String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Registration Successful");
        message.setText("Your registration has been successfully completed. Welcome to our platform!");

        try {
            mailSender.send(message);
            System.out.println("Registration confirmation email sent to " + email);
        } catch (Exception e) {
            System.out.println("Error sending registration confirmation email to " + email);
            e.printStackTrace();
            throw new RuntimeException("Error sending registration confirmation email: " + e.getMessage());
        }
    }
    public ResponseEntity<Set<Roles>> getRolesByUserId(String UserId ) {
        UserDetails user = userDao.findByUserId(UserId);
        Set<Roles> roles = user.getRoles();
        return  ResponseEntity.ok(roles);

    }

//    public List<EmployeeWithRole> getRolesId(long id) {
//        List<UserDetails> list = userDao.findByRolesId(id);
//
//        if (list.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        return list.stream()
//                .map(e -> new EmployeeWithRole(e.getUserId(), e.getUserName(),e.getRoles()))
//                .collect(Collectors.toList());
//    }
//
//    public ResponseEntity<List<EmployeeWithRole>> getAllEmployeesWithRoles() {
//        List<UserDetails> users = userDao.findAll();
//        if (users.isEmpty()) {
//            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//        }
//
//        List<EmployeeWithRole> employeeRoles = users.stream()
//                .map(user -> new EmployeeWithRole(
//                        user.getUserId(),
//                        user.getUserName(),
//                        user.getRoles()
//                ))
//                .collect(Collectors.toList());
//
//        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
//    }


    public ResponseEntity<List<EmployeeWithRole>> getAllEmployeesWithRoles() {
        // Fetch all users from the database
        List<UserDetails> users = userDao.findAll();

        // If no users are found, return a No Content response
        if (users.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        // Map each UserDetails to an EmployeeWithRole, flattening the roles into a single string
        List<EmployeeWithRole> employeeRoles = users.stream()
                .map(user -> {
                    // Ensure that user.getRoles() returns a valid Set<Roles> and handle empty roles correctly
                    String roleName = Optional.ofNullable(user.getRoles())  // Null check for user roles
                            .flatMap(roles -> roles.stream()
                                    .map(role -> role.getName().name())  // Access enum name as String
                                    .findFirst())  // Get the first role name if present
                            .orElse("No Role");  // Default to "No Role" if no roles are found or empty

                    // Create and return the EmployeeWithRole object
                    return new EmployeeWithRole(
                            user.getUserId(),       // Set user ID
                            user.getUserName(),
                            // Set user name
                            roleName  ,
                            user.getEmail(),
                            user.getDesignation(),
                            user.getJoiningDate(),
                            user.getGender(),
                            user.getDob(),
                            user.getPhoneNumber(),
                            user.getPersonalemail(),
                            user.getStatus()// Set role/ Set role name as a simple string
                    );
                })
                .collect(Collectors.toList());

        // Return the list of EmployeeWithRole objects with an OK response status
        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    public ResponseEntity<List<EmployeeWithRole>> getEmployeesByUserIdAndRole(String userId, String roleName) {
        UserType roleEnum = null;
        if (roleName != null && !roleName.isBlank()) {
            try {
                roleEnum = UserType.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid role name provided: {}", roleName);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        List<UserDetails> users;

        if ((userId == null || userId.isBlank()) && roleEnum == null) {
            users = userDao.findAll();
            logger.info("Fetching all users. Total: {}", users.size());
        } else {
            users = userDao.findByUserIdAndRole(userId, roleEnum);
            logger.info("Fetching users by filters - userId: {}, role: {}. Total found: {}",
                    userId, roleEnum != null ? roleEnum.name() : "ANY", users.size());
        }

        if (users.isEmpty()) {
            logger.info("No users found for the given criteria.");
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

        // 🔍 Log how many users belong to each role
        Map<String, Long> roleCounts = users.stream()
                .flatMap(u -> u.getRoles().stream())
                .map(role -> role.getName().name())
                .collect(Collectors.groupingBy(roleNameStr -> roleNameStr, Collectors.counting()));

        roleCounts.forEach((role, count) ->
                logger.info("Role: {} - User count: {}", role, count));

        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }


    public ResponseEntity<List<EmployeeWithRole>> getEmployeesWithFlexibleRoleFilter(String userId, String roleName, String excludeRoleName) {
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
            users = userDao.findByUserIdAndRoleNot(userId, excludeRole);

            logger.info("Fetched {} users with userId={} excluding role={}, status='ACTIVE' and designation<>'testuser'",
                    users.size(), userId, excludeRole);

        } else if (includeRole != null) {
            users = userDao.findByUserIdAndRole(userId, includeRole);

            logger.info("Fetched {} users with userId={} and role={}, status='ACTIVE' and designation<>'testuser'",
                    users.size(), userId, includeRole);

        } else {
            users = (userId == null || userId.isBlank()) ? userDao.findAllActiveNonTestUsers() : userDao.findByUserIdAndRole(userId, null);

            logger.info("Fetched {} users with userId={}, no role filter, status='ACTIVE' and designation<>'testuser'",
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
}


















