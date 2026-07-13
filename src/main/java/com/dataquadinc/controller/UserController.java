package com.dataquadinc.controller;

import com.dataquadinc.dto.*;
import com.dataquadinc.exceptions.DateRangeValidationException;
import com.dataquadinc.exceptions.UserNotFoundException;

import com.dataquadinc.model.EmployeeAttendance;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.management.relation.RoleNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


//@CrossOrigin(origins = "http://35.188.150.92")
////@CrossOrigin(origins = "http://192.168.0.139:3000")
////git status



@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com","http://localhost:3000","http://192.168.0.135:8080","http://192.168.0.135",
        "http://154.210.288.26",
        "http://192.168.0.203:3000",
        "http://192.168.0.167:3000"})
@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ResponseBean<UserResponse>> registerUser(@Valid @RequestBody UserDto userDto) throws RoleNotFoundException {

        return userService.registerUser(userDto);

    }


    @GetMapping("/{userId}/email")
    public ResponseEntity<String> getRecruiterEmail(@PathVariable String userId) {
        try {
            // Fetch the recruiter details using the userId
            UserDetails recruiter = userService.getRecruiterById(userId);

            // Log the recruiter object
            System.out.println("Fetched recruiter: " + recruiter);

            if (recruiter == null) {
                throw new UserNotFoundException("Recruiter not found with ID: " + userId);
            }

            // Log the email being returned
            System.out.println("Email: " + recruiter.getEmail());

            // Return the email as a plain response
            return ResponseEntity.ok(recruiter.getEmail());

        } catch (UserNotFoundException ex) {
            // Log the error
            System.out.println("Error: " + ex.getMessage());

            // Return an error response with a suitable status code and message
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: " + ex.getMessage());
        }
    }

    @GetMapping("/{userIds}/username")
    public ResponseEntity<String> getRecruiterUsername(@PathVariable String userIds) {
        try {
            // Split the userIds string into individual IDs
            String[] idArray = userIds.split(",");
            StringBuilder result = new StringBuilder();
            List<String> notFoundIds = new ArrayList<>();

            // Process each ID individually
            for (String userId : idArray) {
                try {
                    UserDetails recruiter = userService.getRecruiterById(userId.trim());

                    if (recruiter != null) {
                        // If not first entry, add a comma
                        if (result.length() > 0) {
                            result.append(",");
                        }
                        result.append(recruiter.getUserName());
                    } else {
                        notFoundIds.add(userId.trim());
                    }
                } catch (UserNotFoundException ex) {
                    notFoundIds.add(userId.trim());
                }
            }

            // If we found no valid users
            if (result.length() == 0) {
                throw new UserNotFoundException("Recruiters not found with IDs: " + String.join(",", notFoundIds));
            }

            // Log the final result
            System.out.println("Usernames found: " + result.toString());
            if (!notFoundIds.isEmpty()) {
                System.out.println("IDs not found: " + String.join(",", notFoundIds));
            }

            return ResponseEntity.ok(result.toString());

        } catch (UserNotFoundException ex) {
            // Log the error
            System.out.println("Error: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: " + ex.getMessage());
        } catch (Exception ex) {
            // Log any unexpected errors
            System.out.println("Unexpected error: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: An unexpected error occurred");
        }
    }


    @PostMapping("/addusers")
    public ResponseEntity<ResponseBean<UserResponse>> registerUsers(@Valid @RequestBody UserDto userDto) throws RoleNotFoundException {

        return userService.registerUser(userDto);

    }

    @GetMapping("/{userId}/login-status")
    public ResponseEntity<ApiResponse<UserLoginStatusDTO>> getLoginStatusByUserId(@PathVariable String userId) {
        UserLoginStatusDTO loginStatus = userService.getLoginStatusByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Login status fetched", loginStatus));
    }


    @GetMapping("/email")
    public ResponseEntity<ApiResponse<UserDetailsDTO>> getUserByEmail(@RequestParam String email) {
        UserDetailsDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("User fetched by email", user));
    }


    @GetMapping("/employee")
    public ResponseEntity<List<EmployeeWithRole>> getAllEmployees(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String excludeRoleName,
            @RequestParam(required = false) String entity) {

        ResponseEntity<List<EmployeeWithRole>> responseEntity =
                userService.getEmployeesWithFlexibleRoleFilter(userId, roleName, excludeRoleName, entity);

        List<EmployeeWithRole> employeeRoles = responseEntity.getBody();

        if (employeeRoles == null || employeeRoles.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    @GetMapping("/active-internal/employee")
    public ResponseEntity<List<EmployeeWithRole>> getAllInternalEmployees() {

        ResponseEntity<List<EmployeeWithRole>> responseEntity =
                userService.findAllActiveInternal();

        List<EmployeeWithRole> employeeRoles = responseEntity.getBody();

        if (employeeRoles == null || employeeRoles.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    @GetMapping("/active-external/employee")
    public ResponseEntity<List<EmployeeWithRole>> getAllExtarnalEmployeesV() {

        ResponseEntity<List<EmployeeWithRole>> responseEntity =
                userService.findAllActiveExternal();

        List<EmployeeWithRole> employeeRoles = responseEntity.getBody();

        if (employeeRoles == null || employeeRoles.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    @GetMapping("/inactive-internal/employee")
    public ResponseEntity<List<EmployeeWithRole>> getAllInactiveInternalEmployees() {

        ResponseEntity<List<EmployeeWithRole>> responseEntity =
                userService.findAllInActiveInternal();

        List<EmployeeWithRole> employeeRoles = responseEntity.getBody();

        if (employeeRoles == null || employeeRoles.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    @GetMapping("/inactive-external/employee")
    public ResponseEntity<List<EmployeeWithRole>> getAllInactiveExtarnalEmployeesV() {

        ResponseEntity<List<EmployeeWithRole>> responseEntity =
                userService.findAllInActiveExternal();

        List<EmployeeWithRole> employeeRoles = responseEntity.getBody();

        if (employeeRoles == null || employeeRoles.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
    }

    @GetMapping("/employee/filterByJoiningDate")
    public ResponseEntity<?> getEmployeesByJoiningDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            List<EmployeeWithRole> employees = userService.getEmployeesByJoiningDateRange(startDate, endDate);

            if (employees.isEmpty()) {
                log.warn("⚠️ No employees found between {} and {}", startDate, endDate);
                return new ResponseEntity<>(new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        "No employees found between " + startDate + " and " + endDate,
                        LocalDateTime.now()), HttpStatus.NOT_FOUND);
            }

            log.info("✅ Fetched {} employees between {} and {}", employees.size(), startDate, endDate);
            return new ResponseEntity<>(employees, HttpStatus.OK);

        } catch (DateRangeValidationException ex) {
            log.error("❌ Date range validation failed: {}", ex.getMessage());
            return new ResponseEntity<>(new com.dataquadinc.dto.ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    ex.getMessage(),
                    LocalDateTime.now()), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping(value = "/update/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBean<UserResponse>> updateUser(@PathVariable String userId, @Valid @RequestBody UserDto userDto) throws RoleNotFoundException {
        return userService.updateUser(userId, userDto);
    }

    @PutMapping(value = "/update/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseBean<UserResponse>> updateUserWithProfilePhoto(
            @PathVariable String userId,
            @RequestParam MultiValueMap<String, String> formFields,
            @RequestPart(value = "profilePhoto", required = false) MultipartFile profilePhoto,
            @RequestPart(value = "documentFiles", required = false) List<MultipartFile> documentFiles,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents) {
        return userService.updateUserMultipart(userId, formFields, profilePhoto, documentFiles, documents);
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<ResponseBean<UserResponse>> deleteUser(@PathVariable String userId) {

        return userService.deleteUser(userId);

    }

    @GetMapping("/bdmlist")
    public ResponseEntity<List<BdmEmployeeDTO>> getBdmEmployees() {
        List<BdmEmployeeDTO> bdmEmployees = userService.getAllBdmEmployees();
        if (bdmEmployees.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(bdmEmployees, HttpStatus.OK);
    }

    // Endpoint to get the total submissions count across all clients and jobs
    @GetMapping("/total-submissions")
    public ResponseEntity<Long> getTotalSubmissions() {
        // Get total submissions using the service method
        long totalSubmissions = userService.getTotalSubmissionsAcrossAllClientsAndJobs();

        // Return the count as a response
        return ResponseEntity.ok(totalSubmissions);
    }

    @GetMapping("/bdmlist/filterByDate")
    public ResponseEntity<List<BdmEmployeeDTO>> getBdmEmployeesDateFilter(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<BdmEmployeeDTO> bdmEmployees = userService.getAllBdmEmployeesDateFilter(startDate, endDate);
        if (bdmEmployees.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(bdmEmployees, HttpStatus.OK);
    }

    @GetMapping("/allUsers")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        ApiResponse<List<UserDto>> apiResponse = new ApiResponse<>(true, "Data Fetched", users, null);

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping("/allUsers/filters")
    public ResponseEntity<Page<UserDto>> getAllFilteredUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joiningDate,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Pageable pageable = PageRequest.of(page, size,
                sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

        Page<UserDto> users = userService.getAllFilteredUsers(userId, userName, email, joiningDate, pageable);
        return ResponseEntity.ok(users);
    }


    @GetMapping("/user/{userId}")
    ResponseEntity<ApiResponse<UserDto>> getUserByUserID(@PathVariable String userId) {
        UserDto userDto = userService.getUserByUserId(userId);
        ApiResponse<UserDto> apiResponse = new ApiResponse<>(true, "Data Fetched", userDto, null);

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/user/userId-userName")
    public ResponseEntity<List<UserAssignment>> getUserIdsAndUserNames(
            @RequestBody List<String> userIds
    ) {
        return new ResponseEntity<>(userService.getUserIdsAndUserNames(userIds), HttpStatus.OK);
    }

    @GetMapping("/users-dropdown")
    public ResponseEntity<ApiResponse<List<UserAssignment>>> getUsersDropdown() {

        ApiResponse<List<UserAssignment>> apiResponse = new ApiResponse<>(true, "Users Data Fetched Successful", userService.getUsersDropdown(), null);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping("/usernameByRole/{userId}")
    public ResponseEntity<Map<String, Object>> getUserRoleAndUsername(@PathVariable String userId) {
        Map<String, Object> userInfo = userService.getUserRoleAndUsername(userId);
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/user-creds/{userId}")
    public ResponseEntity<UserDetails> getUserRole(@PathVariable String userId) {
        UserDetails data = userService.getUserCreds(userId);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/attendance/month/setup")
    public ResponseEntity<?> setupAttendanceMonth(
            @RequestBody AttendanceMonthSetupDto dto) {

        try {

            String response = userService.setupAttendanceMonth(dto);

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            response,
                            null,
                            null
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(
                            false,
                            e.getMessage(),
                            null,
                            null
                    )
            );
        }
    }

    @PostMapping("/attendance/save")
    public ResponseEntity<?> saveAttendance(
            @RequestBody AttendanceSaveRequestDto dto) {

        try {

            String response = userService.saveAttendance(dto);

            return ResponseEntity.ok(new ApiResponse<>(
                            true,
                            response,
                            null,
                            null
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(
                            false,
                            e.getMessage(),
                            null,
                            null
                    )
            );
        }
    }


    @GetMapping("/attendance/dashboard")
    public ResponseEntity<?> getAttendanceDashboard(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam String entity) {

        try {

            List<AttendanceDashboardResponseDto> response =
                    userService.getAttendanceDashboard(month, year, entity);

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            "Attendance dashboard fetched successfully",
                            response,
                            null
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(
                            false,
                            e.getMessage(),
                            null,
                            null
                    )
            );
        }
    }
    @GetMapping("/attendance/employee")
    public ResponseEntity<?> getEmployeeAttendance(
            @RequestParam String employeeId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam String entity) {

        try {

            List<EmployeeAttendanceViewDto> response =
                    userService.getEmployeeAttendance(
                            employeeId,
                            month,
                            year,
                            entity);

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            "Employee attendance fetched successfully",
                            response,
                            null
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(
                            false,
                            e.getMessage(),
                            null,
                            null
                    )
            );
        }
    }
    @PutMapping("/attendance/day/edit")
    public ResponseEntity<?> editAttendance(
            @RequestBody AttendanceSaveRequestDto dto) {

        try {
            String response = userService.editAttendance(dto);

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            response,
                            null,
                            null
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(
                            false,
                            e.getMessage(),
                            null,
                            null
                    )
            );
        }
    }
    @PutMapping("/attendance/month/edit")
    public ResponseEntity<?> editAttendanceMonth(
            @RequestBody AttendanceMonthSetupDto dto,
            @RequestParam String entity) {

        try {

            String response = userService.editAttendanceMonth(dto, entity);

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            response,
                            null,
                            null
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(
                            false,
                            e.getMessage(),
                            null,
                            null
                    )
            );
        }
    }
    @PostMapping("/attendance/week/submit")
    public ResponseEntity<?> submitWeekAttendance(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam Integer weekNumber) {

        try {

            String response = userService.submitWeekAttendance(month, year, weekNumber);
            return ResponseEntity.ok(new ApiResponse<>(
                            true,
                            response,
                            null,
                            null)
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(false,
                            e.getMessage(),
                            null,
                            null
                    )
            );
        }
    }
    @PostMapping("/attendance/week/approve")
    public ResponseEntity<?> approveWeekAttendance(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam Integer weekNumber) {

        try {

            String response = userService.approveWeekAttendance(month, year, weekNumber);

            return ResponseEntity.ok(new ApiResponse<>(
                            true,
                            response,
                            null,
                            null
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                            false,
                            e.getMessage(),
                            null,
                            null
                    )
            );
        }
    }

    @PostMapping("/attendance/week/reject")
    public ResponseEntity<?> rejectWeekAttendance(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam Integer weekNumber) {

        try {
            String response = userService.rejectWeekAttendance(month, year, weekNumber);

            return ResponseEntity.ok(new ApiResponse<>(
                            true,
                            response,
                            null,
                            null
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                            false,
                            e.getMessage(),
                            null,
                            null
                    )
            );
        }
    }
    @GetMapping("/attendance/month/holidays")
    public ResponseEntity<?> getAttendanceMonthHolidays(
            @RequestParam Integer month,
            @RequestParam Integer year) {

        try {

            List<LocalDate> response = userService.getAttendanceMonthHolidays(month, year);

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            "Attendance holidays fetched successfully",
                            response,
                            null
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(
                            false,
                            e.getMessage(),
                            null,
                            null
                    )
            );
        }
    }
}