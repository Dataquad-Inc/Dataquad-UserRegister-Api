package com.dataquadinc.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
public class UserDetails {

    @Id
    @Column(unique = true,nullable = false)

    private String userId; // This is set manually from the frontend


    private String userName;




    @Column(nullable = false)
    private String password;


    @Column(nullable=false)
    private String confirmPassword;


    @Column(unique = true, nullable = false)

    private String email;


    @Column(unique = true, nullable = false)

    private String personalemail;

    @NotEmpty

    private String phoneNumber;


    @Column(name = "dob", nullable = false)
    private String dob;



    @Column(name = "gender", nullable = false)
    private String gender;


    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @NotEmpty
    private String designation;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt= LocalDateTime.now();;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt=LocalDateTime.now();;


    @Column(name = "last_Login_Time")
    private LocalDateTime lastLoginTime;

    @Column(nullable = false)
    @ManyToMany
    @JoinTable(
            name = "user_roles", // Name of the join table
            joinColumns = @JoinColumn(name = "user_id"), // Foreign key to UserDetails
            inverseJoinColumns = @JoinColumn(name = "role_id") // Foreign key to Roles
    )

    private Set<Roles> roles = new HashSet<>();
    private String status;

    private String encryptionKey;

    private boolean primarySuperAdmin;

    private String entity;

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public boolean isPrimarySuperAdmin() {
        return primarySuperAdmin;
    }

    public void setPrimarySuperAdmin(boolean primarySuperAdmin) {
        this.primarySuperAdmin = primarySuperAdmin;
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

    public @NotEmpty String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(@NotEmpty String phoneNumber) {
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

    public @NotEmpty String getDesignation() {
        return designation;
    }

    public void setDesignation(@NotEmpty String designation) {
        this.designation = designation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public Set<Roles> getRoles() {
        return roles;
    }

    public void setRoles(Set<Roles> roles) {
        this.roles = roles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status.toUpperCase();
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
}