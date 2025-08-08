package com.dataquadinc.controller;

import com.dataquadinc.dto.LoginDTO;
import com.dataquadinc.dto.LoginResponseDTO;
import com.dataquadinc.exceptions.InvalidCredentialsException;
import com.dataquadinc.exceptions.UserAlreadyLoggedInException;
import com.dataquadinc.exceptions.UserNotFoundException;
import com.dataquadinc.exceptions.UserInactiveException; // Import the UserInactiveException
import com.dataquadinc.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000", "https://mymulya.com", "http://localhost:3000","http://192.168.0.135:8080","http://192.168.0.135",
        "http://182.18.177.16","http://192.168.1.151:3000"})
@RestController
@RequestMapping("/users")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        LoginResponseDTO response = loginService.authenticate(loginDTO);
        return ResponseEntity.ok(response);
    }

    // Handle Invalid Credentials Exception (returns 401 Unauthorized)
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<LoginResponseDTO> handleInvalidCredentials(InvalidCredentialsException e) {
        LoginResponseDTO errorResponse = new LoginResponseDTO(
                false,
                "Unsuccessful",
                null,
                new LoginResponseDTO.ErrorDetails("300", e.getMessage())
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // Handle Already Logged In Exception (returns 400 Bad Request)
    @ExceptionHandler(UserAlreadyLoggedInException.class)
    public ResponseEntity<LoginResponseDTO> handleUserAlreadyLoggedIn(UserAlreadyLoggedInException e) {
        LoginResponseDTO errorResponse = new LoginResponseDTO(
                false,
                "Unsuccessful",
                null,
                new LoginResponseDTO.ErrorDetails("201", e.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(errorResponse);
    }

    // Handle User Not Found Exception (returns 404 Not Found)
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<LoginResponseDTO> handleUserNotFound(UserNotFoundException e) {
        LoginResponseDTO errorResponse = new LoginResponseDTO(
                false,
                "Unsuccessful",
                null,
                new LoginResponseDTO.ErrorDetails("404", e.getMessage())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // Handle User Inactive Exception (returns 403 Forbidden)
    @ExceptionHandler(UserInactiveException.class)
    public ResponseEntity<LoginResponseDTO> handleUserInactiveException(UserInactiveException e) {
        LoginResponseDTO errorResponse = new LoginResponseDTO(
                false,
                "Unsuccessful",
                null,
                new LoginResponseDTO.ErrorDetails("403", e.getMessage()) // 403 for forbidden action
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse); // Return 403 Forbidden status
    }
}
