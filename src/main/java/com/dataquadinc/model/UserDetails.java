package com.dataquadinc.model;

import com.dataquadinc.dto.TeamAssignment;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
public class UserDetails {

    @Id
    private String userId;

    private String userName;

    @Column(nullable = false)
    private String password;

    private String confirmPassword;


    @Column(unique = true, nullable = false)
    private String email;

    private String personalemail;

    private String phoneNumber;

    private String dob;

    private String gender;

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

    private String associatedTeamLeadId;

    private String entity;

    private String teamName;

    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    private List<TeamAssignment> teamAssignments=new ArrayList<>();

    private String emergencyContactNumber;
    private String currentAddress;
    private String permanentAddress;
    private String linkedinUrl;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] profilePhoto;

    private String profilePhotoFileName;
    private String profilePhotoContentType;

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getAssociatedTeamLeadId() {
        return associatedTeamLeadId;
    }

    public void setAssociatedTeamLeadId(String associatedTeamLeadId) {
        this.associatedTeamLeadId = associatedTeamLeadId;
    }

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

    public List<TeamAssignment> getTeamAssignments() {
        if (teamAssignments == null) {
            teamAssignments = new ArrayList<>();
        }
        return teamAssignments;
    }


    public void setTeamAssignments(List<TeamAssignment> teamAssignments) {
        this.teamAssignments = teamAssignments;
    }


    public void addTeamAssignmentIfNotExists(TeamAssignment newAssignment) {

        boolean exists = this.teamAssignments.stream()
                .anyMatch(t -> t.getTeamLeadId().equals(newAssignment.getTeamLeadId())
                        && t.getTeamName().equalsIgnoreCase(newAssignment.getTeamName()));

        if (!exists) {
            List<TeamAssignment> updated = new ArrayList<>(this.teamAssignments);
            updated.add(newAssignment);
            this.teamAssignments = updated;
        }
    }

    public String getEmergencyContactNumber() {
        return emergencyContactNumber;
    }

    public void setEmergencyContactNumber(String emergencyContactNumber) {
        this.emergencyContactNumber = emergencyContactNumber;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }

    public void setCurrentAddress(String currentAddress) {
        this.currentAddress = currentAddress;
    }

    public String getPermanentAddress() {
        return permanentAddress;
    }

    public void setPermanentAddress(String permanentAddress) {
        this.permanentAddress = permanentAddress;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public byte[] getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(byte[] profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public String getProfilePhotoFileName() {
        return profilePhotoFileName;
    }

    public void setProfilePhotoFileName(String profilePhotoFileName) {
        this.profilePhotoFileName = profilePhotoFileName;
    }

    public String getProfilePhotoContentType() {
        return profilePhotoContentType;
    }

    public void setProfilePhotoContentType(String profilePhotoContentType) {
        this.profilePhotoContentType = profilePhotoContentType;
    }
}