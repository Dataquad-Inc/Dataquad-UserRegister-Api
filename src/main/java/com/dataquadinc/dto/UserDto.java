package com.dataquadinc.dto;


import com.dataquadinc.model.UserType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UserDto {

        @Id
//        @Size( max = 8, message = "User ID must be between 5 and 20 characters")
        private String userId;

        @NotEmpty( message="userName can not be empty")
        //@Size(min = 8, max = 20, message = "User name must be between 2 and 50 characters")
        private String userName;


        @NotEmpty(message = "Password must not be empty")
        //@Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
        private String password;

        @NotEmpty

        @NotEmpty(message = "Password must not be empty")
        @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
        private String confirmPassword;


        @Email
        @Column(unique = true, nullable = false)
        @NotEmpty(message = "Email must not be empty")
        //@Size(min = 20, max = 50, message = "email must be between 20 and 50 characters")
        private String email;

        @Email
        @Column(unique = true, nullable = false)
        @NotEmpty(message = "Email must not be empty")
        //@Size(min = 20, max = 50, message = "email must be between 20 and 50 characters")
        private String personalemail;

        @NotEmpty
        @Pattern(regexp = "^[0-9]{10}$", message = "Invalid phone number")
        private String phoneNumber;

        @Column(name = "dob", nullable = false)
    //    @Past(message = "Date of birth must be in the past")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private String dob;

        @Column(name = "gender", nullable = false)
        @Pattern(regexp = "Male|Female", message = "Gender must be Male, Female")
        private String gender;

    //    @Past(message = "Date of birth must be in the past")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Column(name = "joining_date", nullable = false)
        @NotNull
        private LocalDate joiningDate;


        @NotEmpty
        private String designation;


        private Set<UserType> roles;

    private String status;
    private String entity;

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public UserDto() {
        this.status = "ACTIVE";  // Default value for status
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPersonalemail() {
        return personalemail;
    }

    public void setPersonalemail(String personalemail) {
        this.personalemail = personalemail;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Set<UserType> getRoles() {
        return roles;
    }

    public void setRoles(Set<UserType> roles) {
        this.roles = roles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}