package com.dataquadinc.service;

import com.dataquadinc.dto.LoginDTO;
import com.dataquadinc.dto.LoginResponseDTO;
import com.dataquadinc.exceptions.InvalidCredentialsException;
import com.dataquadinc.exceptions.UserAlreadyLoggedInException;
import com.dataquadinc.exceptions.UserInactiveException;
import com.dataquadinc.exceptions.UserNotFoundException;  // Import the new exception
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserType;
import com.dataquadinc.repository.LoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class LoginService {

    @Autowired
    private LoginRepository loginRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public LoginResponseDTO authenticate(LoginDTO loginDTO) {
        // Check if user exists in the database
        UserDetails userDetails = loginRepository.findByEmail(loginDTO.getEmail());

        if (userDetails == null) {
            // If user is not found, throw a UserNotFoundException
            throw new UserNotFoundException("User not found with email: " + loginDTO.getEmail());
        }

        // Check if the user is active
        if (userDetails.getStatus() == null || !userDetails.getStatus().equals("ACTIVE")) {
            // If user is not active, throw a UserInactiveException
            throw new UserInactiveException("User is inactive and cannot log in.");
        }

        // Validate password
        if (!passwordEncoder.matches(loginDTO.getPassword(), userDetails.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Ensure roles are present before proceeding
        if (userDetails.getRoles() == null || userDetails.getRoles().isEmpty()) {
            throw new InvalidCredentialsException("No roles assigned to the user");
        }

//        // Define the session timeout duration (e.g., 30 minutes)
//        Duration sessionTimeout = Duration.ofMinutes(30);
//
//        // If the user has logged out or session expired, reset the login time
//        if (userDetails.getLastLoginTime() != null) {
//            Duration timeSinceLastLogin = Duration.between(userDetails.getLastLoginTime(), LocalDateTime.now());
//            if (timeSinceLastLogin.compareTo(sessionTimeout) >= 0) {
//                // The session has expired or the user has logged out, allow login
//                userDetails.setLastLoginTime(null); // Clear the last login time, session expired
//            } else {
//                // If the session is still active, prevent login
//                throw new UserAlreadyLoggedInException("User is already logged in and session is active.");
//            }
//        }

        // If the session has expired or the user is logging in for the first time, proceed with login
        // Update the last login time
        userDetails.setLastLoginTime(LocalDateTime.now());
        loginRepository.save(userDetails);

        // Get the first role assigned to the user (assuming at least one role is present)
        UserType roleType = userDetails.getRoles().iterator().next().getName();


                String encoded= Base64.getEncoder().encodeToString(userDetails.getEncryptionKey().getBytes());
        // Create payload with the user's details and role
        LoginResponseDTO.Payload payload = new LoginResponseDTO.Payload(
                userDetails.getUserId(),
                userDetails.getUserName(),
                userDetails.getEmail(),
                roleType,
                userDetails.getLastLoginTime(),
                encoded,
                userDetails.getEntity()
        );

        // Return successful login response
        return new LoginResponseDTO(true, "Login successful", payload);
    }
}