package com.dataquadinc.dto;

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


}
