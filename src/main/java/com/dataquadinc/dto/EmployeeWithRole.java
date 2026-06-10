package com.dataquadinc.dto;

import com.dataquadinc.model.UserDetails;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class EmployeeWithRole
{
    @JsonProperty("employeeId")
    private String employeeId;
    @JsonProperty("userName")
    private String employeeName;
    @JsonProperty("roles")
    private String Roles;
    @JsonProperty("email")
    private String email;
    @JsonProperty("designation")
    private String designation;
    @JsonProperty("joiningDate")
    private LocalDate joiningDate;
    @JsonProperty("gender")
    private String gender;
    @JsonProperty("dob")
    private String dob;
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    @JsonProperty("personalemail")
    private String personalemail;
    @JsonProperty("status")
    private String status;
    @JsonProperty("isEditable")
    private Boolean isEditable;
    private String fatherOrSpouseName;
    private String motherName;
    private String bloodGroup;
    private String maritalStatus;
    private String currentAddress;
    private String permanentAddress;
    private String emergencyContactNo;
    private String officialNumber;
    private String officialEmailId;
    private String probation;
    private String reportingManager;
    private String department;
    private String linkedInUrl;
    private String bankName;
    private String accountNumber;
    private String branch;
    private String accountHolderName;
    private String ifscCode;
    private Boolean isEmployeeHavingPF;
    private Boolean isEmployeeHavingESI;
    private String esiNumber;
    private String uanNumber;
    private String pfNumber;
    private String payrollPanNumber;
    private String payrollAadharNumber;
    private String fAndF;
    private LocalDate exitFromPfDate;
    private LocalDate lastWorkingDay;
    private String pan;
    private String adhar;
    private String entity;


//    public EmployeeWithRole(String employeeId, String employeeName, String roles,String email) {
//        this.employeeId = employeeId;
//        this.employeeName = employeeName;
//        this.Roles = roles;
//        this.email=email;
//    }

    public EmployeeWithRole(String employeeId, String employeeName, String roles, String email, String designation, LocalDate joiningDate, String gender, String dob, String phoneNumber, String personalemail,String status, Boolean isEditable) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        Roles = roles;
        this.email = email;
        this.designation = designation;
        this.joiningDate = joiningDate;
        this.gender = gender;
        this.dob = dob;
        this.phoneNumber = phoneNumber;
        this.personalemail = personalemail;
        this.status=status;
        this.isEditable = Boolean.TRUE.equals(isEditable);
    }

    public static EmployeeWithRole fromUserDetails(UserDetails user, String roles) {
        EmployeeWithRole employee = new EmployeeWithRole(
                user.getUserId(),
                user.getUserName(),
                roles,
                user.getEmail(),
                user.getDesignation(),
                user.getJoiningDate(),
                user.getGender(),
                user.getDob(),
                user.getPhoneNumber(),
                user.getPersonalemail(),
                user.getStatus(),
                user.getIsEditable()
        );

        employee.setFatherOrSpouseName(user.getFatherOrSpouseName());
        employee.setMotherName(user.getMotherName());
        employee.setBloodGroup(user.getBloodGroup());
        employee.setMaritalStatus(user.getMaritalStatus());
        employee.setCurrentAddress(user.getCurrentAddress());
        employee.setPermanentAddress(user.getPermanentAddress());
        employee.setEmergencyContactNo(user.getEmergencyContactNumber());
        employee.setOfficialNumber(user.getOfficialNumber());
        employee.setOfficialEmailId(user.getOfficialEmailId());
        employee.setProbation(user.getProbation());
        employee.setReportingManager(user.getReportingManager());
        employee.setDepartment(user.getDepartment());
        employee.setLinkedInUrl(user.getLinkedinUrl());
        employee.setBankName(user.getBankName());
        employee.setAccountNumber(user.getAccountNumber());
        employee.setBranch(user.getBranch());
        employee.setAccountHolderName(user.getAccountHolderName());
        employee.setIfscCode(user.getIfscCode());
        employee.setIsEmployeeHavingPF(user.getIsEmployeeHavingPF());
        employee.setIsEmployeeHavingESI(user.getIsEmployeeHavingESI());
        employee.setEsiNumber(user.getEsiNumber());
        employee.setUanNumber(user.getUanNumber());
        employee.setPfNumber(user.getPfNumber());
        employee.setPayrollPanNumber(user.getPayrollPanNumber());
        employee.setPayrollAadharNumber(user.getPayrollAadharNumber());
        employee.setFAndF(user.getFAndF());
        employee.setExitFromPfDate(user.getExitFromPfDate());
        employee.setLastWorkingDay(user.getLastWorkingDay());
        employee.setPan(user.getPan());
        employee.setAdhar(user.getAdhar());
        employee.setEntity(user.getEntity());
        return employee;
    }

}
