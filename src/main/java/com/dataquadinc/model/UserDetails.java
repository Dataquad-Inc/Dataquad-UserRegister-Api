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

    private String fatherOrSpouseName;

    private String motherName;

    private String bloodGroup;

    private String gender;

    private String maritalStatus;

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
    private LocalDate doj;
    private String officialNumber;
    private String officialEmailId;
    private String probation;
    private String reportingManager;
    private String department;
    private String bankName;
    private String accountNumber;
    private String branch;
    private String accountHolderName;
    private String ifscCode;
    private String uanNumber;
    private String pfNumber;
    private String payrollPanNumber;
    private String payrollAadharNumber;
    private String clearnessForm;
    private String fAndF;
    private LocalDate exitFromPfDate;
    private LocalDate lastWorkingDay;
    private String pan;
    private String adhar;

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

    public String getFatherOrSpouseName() {
        return fatherOrSpouseName;
    }

    public void setFatherOrSpouseName(String fatherOrSpouseName) {
        this.fatherOrSpouseName = fatherOrSpouseName;
    }

    public String getMotherName() {
        return motherName;
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
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
        List<TeamAssignment> updated = new ArrayList<>(this.teamAssignments);

        // Find existing assignment with same teamLeadId
        boolean found = false;
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).getTeamLeadId().equals(newAssignment.getTeamLeadId())) {
                // Update existing assignment
                updated.set(i, newAssignment);
                found = true;
                break;
            }
        }

        // If no existing assignment found, add new one
        if (!found) {
            updated.add(newAssignment);
        }

        this.teamAssignments = updated;
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

    public LocalDate getDoj() {
        return doj;
    }

    public void setDoj(LocalDate doj) {
        this.doj = doj;
    }

    public String getOfficialNumber() {
        return officialNumber;
    }

    public void setOfficialNumber(String officialNumber) {
        this.officialNumber = officialNumber;
    }

    public String getOfficialEmailId() {
        return officialEmailId;
    }

    public void setOfficialEmailId(String officialEmailId) {
        this.officialEmailId = officialEmailId;
    }

    public String getProbation() {
        return probation;
    }

    public void setProbation(String probation) {
        this.probation = probation;
    }

    public String getReportingManager() {
        return reportingManager;
    }

    public void setReportingManager(String reportingManager) {
        this.reportingManager = reportingManager;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getUanNumber() {
        return uanNumber;
    }

    public void setUanNumber(String uanNumber) {
        this.uanNumber = uanNumber;
    }

    public String getPfNumber() {
        return pfNumber;
    }

    public void setPfNumber(String pfNumber) {
        this.pfNumber = pfNumber;
    }

    public String getPayrollPanNumber() {
        return payrollPanNumber;
    }

    public void setPayrollPanNumber(String payrollPanNumber) {
        this.payrollPanNumber = payrollPanNumber;
    }

    public String getPayrollAadharNumber() {
        return payrollAadharNumber;
    }

    public void setPayrollAadharNumber(String payrollAadharNumber) {
        this.payrollAadharNumber = payrollAadharNumber;
    }

    public String getClearnessForm() {
        return clearnessForm;
    }

    public void setClearnessForm(String clearnessForm) {
        this.clearnessForm = clearnessForm;
    }

    public String getFAndF() {
        return fAndF;
    }

    public void setFAndF(String fAndF) {
        this.fAndF = fAndF;
    }

    public LocalDate getExitFromPfDate() {
        return exitFromPfDate;
    }

    public void setExitFromPfDate(LocalDate exitFromPfDate) {
        this.exitFromPfDate = exitFromPfDate;
    }

    public LocalDate getLastWorkingDay() {
        return lastWorkingDay;
    }

    public void setLastWorkingDay(LocalDate lastWorkingDay) {
        this.lastWorkingDay = lastWorkingDay;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getAdhar() {
        return adhar;
    }

    public void setAdhar(String adhar) {
        this.adhar = adhar;
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
