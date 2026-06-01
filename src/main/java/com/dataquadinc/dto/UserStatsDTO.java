package com.dataquadinc.dto;

public class UserStatsDTO {

    private String employeeId;
    private String employeeName;
    private String employeeEmail;
    private String role;

    private int numberOfClients;
    private int numberOfRequirements;

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

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public void setEmployeeEmail(String employeeEmail) {
        this.employeeEmail = employeeEmail;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getNumberOfClients() {
        return numberOfClients;
    }

    public void setNumberOfClients(int numberOfClients) {
        this.numberOfClients = numberOfClients;
    }

    public int getNumberOfRequirements() {
        return numberOfRequirements;
    }

    public void setNumberOfRequirements(int numberOfRequirements) {
        this.numberOfRequirements = numberOfRequirements;
    }

    public int getNumberOfSubmissions() {
        return numberOfSubmissions;
    }

    public void setNumberOfSubmissions(int numberOfSubmissions) {
        this.numberOfSubmissions = numberOfSubmissions;
    }

    public int getNumberOfScreenRejects() {
        return numberOfScreenRejects;
    }

    public void setNumberOfScreenRejects(int numberOfScreenRejects) {
        this.numberOfScreenRejects = numberOfScreenRejects;
    }

    public int getNumberOfInterviews() {
        return numberOfInterviews;
    }

    public void setNumberOfInterviews(int numberOfInterviews) {
        this.numberOfInterviews = numberOfInterviews;
    }

    public int getNumberOfPlacements() {
        return numberOfPlacements;
    }

    public void setNumberOfPlacements(int numberOfPlacements) {
        this.numberOfPlacements = numberOfPlacements;
    }

    public int getSelfSubmissions() {
        return selfSubmissions;
    }

    public void setSelfSubmissions(int selfSubmissions) {
        this.selfSubmissions = selfSubmissions;
    }

    public int getSelfInterviews() {
        return selfInterviews;
    }

    public void setSelfInterviews(int selfInterviews) {
        this.selfInterviews = selfInterviews;
    }

    public int getSelfPlacements() {
        return selfPlacements;
    }

    public void setSelfPlacements(int selfPlacements) {
        this.selfPlacements = selfPlacements;
    }

    public int getTeamScreenRejectCount() {
        return teamScreenRejectCount;
    }

    public void setTeamScreenRejectCount(int teamScreenRejectCount) {
        this.teamScreenRejectCount = teamScreenRejectCount;
    }

    public int getTeamSubmissions() {
        return teamSubmissions;
    }

    public void setTeamSubmissions(int teamSubmissions) {
        this.teamSubmissions = teamSubmissions;
    }

    public int getTeamInterviews() {
        return teamInterviews;
    }

    public void setTeamInterviews(int teamInterviews) {
        this.teamInterviews = teamInterviews;
    }

    public int getTeamPlacements() {
        return teamPlacements;
    }

    public void setTeamPlacements(int teamPlacements) {
        this.teamPlacements = teamPlacements;
    }

    private int numberOfSubmissions;
    private int numberOfScreenRejects;
    private int numberOfInterviews;
    private int numberOfPlacements;

    private int selfSubmissions;
    private int selfInterviews;
    private int selfPlacements;

    private int teamScreenRejectCount;
    private int teamSubmissions;
    private int teamInterviews;
    private int teamPlacements;
}
