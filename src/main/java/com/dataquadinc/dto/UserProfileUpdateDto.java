package com.dataquadinc.dto;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public class UserProfileUpdateDto {
    private String userId;
    private String userName;
    private String phoneNumber;
    private String emergencyContactNumber;
    private String currentAddress;
    private String permanentAddress;
    private String linkedinUrl;
    private String linkedInUrl;
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
    private String personalEmail;
    private String personal_email;
    private LocalDate joining_date;
    private String pan;
    private String adhar;

    private MultipartFile profilePhoto; // optional

    private List<UserDocumentDto> documents; // for multiple documents
    private List<String> documentTypes;
    private List<MultipartFile> documentFiles;

    public MultipartFile getProfilePhoto() {
        return profilePhoto;
    }
    public void setProfilePhoto(MultipartFile profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public List<UserDocumentDto> getDocuments() {
        return documents;
    }
    public void setDocuments(List<UserDocumentDto> documents) {
        this.documents = documents;
    }

    public List<String> getDocumentTypes() {
        return documentTypes;
    }

    public void setDocumentTypes(List<String> documentTypes) {
        this.documentTypes = documentTypes;
    }

    public List<MultipartFile> getDocumentFiles() {
        return documentFiles;
    }

    public void setDocumentFiles(List<MultipartFile> documentFiles) {
        this.documentFiles = documentFiles;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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

    public String getLinkedInUrl() {
        return linkedInUrl;
    }

    public void setLinkedInUrl(String linkedInUrl) {
        this.linkedInUrl = linkedInUrl;
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

    public String getPersonalEmail() {
        return personalEmail;
    }

    public void setPersonalEmail(String personalEmail) {
        this.personalEmail = personalEmail;
    }

    public String getPersonal_email() {
        return personal_email;
    }

    public void setPersonal_email(String personal_email) {
        this.personal_email = personal_email;
    }

    public LocalDate getJoining_date() {
        return joining_date;
    }

    public void setJoining_date(LocalDate joining_date) {
        this.joining_date = joining_date;
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
}
