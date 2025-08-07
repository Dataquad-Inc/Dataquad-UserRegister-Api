
package com.dataquadinc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BdmEmployeeDTO {
    @JsonProperty("employeeId")
    private String employeeId;

    @JsonProperty("employeeName")
    private String employeeName;

    @JsonProperty("roles")
    private String roles;

    @JsonProperty("email")
    private String email;

    @JsonProperty("status")
    private String status;

    @JsonProperty("clientCount")
    private long clientCount;

    @JsonProperty("requirementsCount")
    private long requirementsCount;

    @JsonProperty("submissionCount")
    private long submissionCount;

    @JsonProperty("interviewCount")
    private long interviewCount;

    @JsonProperty("placementCount")
    private long placementCount;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getClientCount() {
        return clientCount;
    }

    public void setClientCount(long clientCount) {
        this.clientCount = clientCount;
    }

    public long getRequirementsCount() {
        return requirementsCount;
    }

    public void setRequirementsCount(long requirementsCount) {
        this.requirementsCount = requirementsCount;
    }

    public long getSubmissionCount() {
        return submissionCount;
    }

    public void setSubmissionCount(long submissionCount) {
        this.submissionCount = submissionCount;
    }

    public long getInterviewCount() {
        return interviewCount;
    }

    public void setInterviewCount(long interviewCount) {
        this.interviewCount = interviewCount;
    }

    public long getPlacementCount() {
        return placementCount;
    }

    public void setPlacementCount(long placementCount) {
        this.placementCount = placementCount;
    }
}
