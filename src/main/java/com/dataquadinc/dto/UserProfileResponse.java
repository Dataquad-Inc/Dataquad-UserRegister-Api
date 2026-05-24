package com.dataquadinc.dto;

import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserProfileDocument;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class UserProfileResponse {
    private String userId;
    private String userName;
    private String email;
    private String personal_email;
    private LocalDate joining_date;
    private String phoneNumber;
    private String dob;
    private String fatherOrSpouseName;
    private String motherName;
    private String bloodGroup;
    private String gender;
    private String maritalStatus;
    private String emergencyContactNo;
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
    private String clearanceForm;
    private String fAndF;
    private LocalDate exitFromPfDate;
    private LocalDate lastWorkingDay;
    private String pan;
    private String adhar;
    private byte[] profilePhoto;
    private List<DocumentResponse> documents;

    public UserProfileResponse(UserDetails user, List<UserProfileDocument> docs) {
        this.userId = user.getUserId();
        this.userName = user.getUserName();
        this.email = user.getEmail();
        this.personal_email = user.getPersonalemail();
        this.joining_date = user.getJoiningDate();
        this.phoneNumber = user.getPhoneNumber();
        this.dob = user.getDob();
        this.fatherOrSpouseName = user.getFatherOrSpouseName();
        this.motherName = user.getMotherName();
        this.bloodGroup = user.getBloodGroup();
        this.gender = user.getGender();
        this.maritalStatus = user.getMaritalStatus();
        this.emergencyContactNo = user.getEmergencyContactNumber();
        this.currentAddress = user.getCurrentAddress();
        this.permanentAddress = user.getPermanentAddress();
        this.linkedinUrl = user.getLinkedinUrl();
        this.linkedInUrl = user.getLinkedinUrl();
        this.doj = user.getDoj();
        this.officialNumber = user.getOfficialNumber();
        this.officialEmailId = user.getOfficialEmailId();
        this.probation = user.getProbation();
        this.reportingManager = user.getReportingManager();
        this.department = user.getDepartment();
        this.bankName = user.getBankName();
        this.accountNumber = user.getAccountNumber();
        this.branch = user.getBranch();
        this.accountHolderName = user.getAccountHolderName();
        this.ifscCode = user.getIfscCode();
        this.uanNumber = user.getUanNumber();
        this.pfNumber = user.getPfNumber();
        this.payrollPanNumber = user.getPayrollPanNumber();
        this.payrollAadharNumber = user.getPayrollAadharNumber();
        this.clearnessForm = user.getClearnessForm();
        this.clearanceForm = user.getClearnessForm();
        this.fAndF = user.getFAndF();
        this.exitFromPfDate = user.getExitFromPfDate();
        this.lastWorkingDay = user.getLastWorkingDay();
        this.pan = user.getPan();
        this.adhar = user.getAdhar();
        this.profilePhoto = user.getProfilePhoto();
        this.documents = docs.stream().map(DocumentResponse::new).collect(Collectors.toList());
    }
}


