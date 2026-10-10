package com.dataquadinc.service;

import com.dataquadinc.dto.LoginDTO;
import com.dataquadinc.dto.LoginResponseDTO;
import com.dataquadinc.exceptions.InvalidCredentialsException;
import com.dataquadinc.exceptions.UserInactiveException;
import com.dataquadinc.exceptions.UserNotFoundException;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserType;
import com.dataquadinc.repository.LoginRepository;
import com.dataquadinc.tenant.TenantContext;
import com.dataquadinc.tenant.TenantResolver;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class LoginService {

    private final LoginRepository loginRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public LoginService(LoginRepository loginRepository, BCryptPasswordEncoder passwordEncoder) {
        this.loginRepository = loginRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponseDTO authenticate(LoginDTO loginDTO) {
        // Email domain wins for Aventra (@aventrainc.ai); else header/host; else mymulya
        String tenantId = TenantResolver.resolveForLogin(
                loginDTO.getEmail(),
                TenantContext.getTenantId()
        );
        TenantContext.setTenantId(tenantId);

        UserDetails userDetails = loginRepository.findByEmailAndTenantId(loginDTO.getEmail(), tenantId);

        // Legacy rows / wrong header: fall back to email-only, then enforce tenant match
        if (userDetails == null) {
            userDetails = loginRepository.findByEmail(loginDTO.getEmail());
            if (userDetails != null) {
                String userTenant = userDetails.getTenantId();
                if (userTenant == null || userTenant.isBlank()) {
                    userTenant = TenantResolver.MYMULYA;
                }
                // If email domain says aventra, do not allow a mymulya-only account through as aventra
                if (!userTenant.equalsIgnoreCase(tenantId)
                        && TenantResolver.fromEmail(loginDTO.getEmail()) != null) {
                    throw new UserNotFoundException("User not found with email: " + loginDTO.getEmail());
                }
                // Trust stored tenant when header was default and email domain did not force aventra
                if (!userTenant.equalsIgnoreCase(tenantId)
                        && TenantResolver.fromEmail(loginDTO.getEmail()) == null) {
                    tenantId = userTenant;
                    TenantContext.setTenantId(tenantId);
                } else if (!userTenant.equalsIgnoreCase(tenantId)) {
                    throw new UserNotFoundException("User not found with email: " + loginDTO.getEmail());
                }
            }
        }

        if (userDetails == null) {
            throw new UserNotFoundException("User not found with email: " + loginDTO.getEmail());
        }

        if (userDetails.getStatus() == null || !userDetails.getStatus().equals("ACTIVE")) {
            throw new UserInactiveException("User is inactive and cannot log in.");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), userDetails.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (userDetails.getRoles() == null || userDetails.getRoles().isEmpty()) {
            throw new InvalidCredentialsException("No roles assigned to the user");
        }

        userDetails.setLastLoginTime(LocalDateTime.now());
        if (userDetails.getTenantId() == null || userDetails.getTenantId().isBlank()) {
            userDetails.setTenantId(tenantId);
        }
        loginRepository.save(userDetails);

        String effectiveTenant = userDetails.getTenantId() != null
                ? userDetails.getTenantId()
                : tenantId;

        UserType roleType = userDetails.getRoles().iterator().next().getName();
        String encoded = Base64.getEncoder().encodeToString(userDetails.getEncryptionKey().getBytes());

        LoginResponseDTO.Payload payload = new LoginResponseDTO.Payload(
                userDetails.getUserId(),
                userDetails.getUserName(),
                userDetails.getEmail(),
                roleType,
                userDetails.getLastLoginTime(),
                encoded,
                userDetails.getEntity(),
                effectiveTenant
        );

        return new LoginResponseDTO(true, "Login successful", payload);
    }

}
