package com.dataquadinc.controller;

import com.dataquadinc.dto.*;
import com.dataquadinc.exceptions.DateRangeValidationException;
import com.dataquadinc.exceptions.UserNotFoundException;
import com.dataquadinc.model.Roles;

import com.dataquadinc.model.UserDetails;
import com.dataquadinc.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.management.relation.RoleNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


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
    public ResponseEntity<ResponseBean<UserResponse>> registerUser(@Valid  @RequestBody UserDto userDto) throws RoleNotFoundException {

         return   userService.registerUser(userDto);

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
    public ResponseEntity<ResponseBean<UserResponse>> registerUsers(@Valid  @RequestBody UserDto userDto) throws RoleNotFoundException {

        return   userService.registerUser(userDto);

    }
    @GetMapping("/roles/{userId}")
    public ResponseEntity<Set<Roles>> getRolesByUserId(@PathVariable String userId ) {
        return userService.getRolesByUserId(userId);
    }

//    @GetMapping("/employee")
//    public ResponseEntity<List<EmployeeWithRole>> getAllEmployees() {
//        ResponseEntity<List<EmployeeWithRole>> responseEntity = userService.getAllEmployeesWithRoles();
//        List<EmployeeWithRole> employeeRoles = responseEntity.getBody();
//        if (employeeRoles == null || employeeRoles.isEmpty()) {
//            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//        }
//        return new ResponseEntity<>(employeeRoles, HttpStatus.OK);
//    }

    @GetMapping("/employee")
    public ResponseEntity<List<EmployeeWithRole>> getAllEmployees(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String excludeRoleName) {

        ResponseEntity<List<EmployeeWithRole>> responseEntity =
                userService.getEmployeesWithFlexibleRoleFilter(userId, roleName, excludeRoleName);

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

    @PutMapping("/update/{userId}")
    public ResponseEntity<ResponseBean<UserResponse>> updateUser(@PathVariable String userId, @Valid @RequestBody UserDto userDto) throws RoleNotFoundException {
        return userService.updateUser(userId, userDto);
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
        List<BdmEmployeeDTO> bdmEmployees = userService.getAllBdmEmployeesDateFilter(startDate,endDate);
        if (bdmEmployees.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(bdmEmployees, HttpStatus.OK);
    }

    @PostMapping("/changeUserId")
    public String changeUserId(){

      return userService.changeUserId();
    }

}
