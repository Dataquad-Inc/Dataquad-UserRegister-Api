package com.dataquadinc.dto;

import java.util.ArrayList;
import java.util.List;

public class AssociatedToTeamLeadResponse {

    private String teamName;
    private String teamLeadId;
    private String teamLeadName;
    List<AssociatedUser> salesExecutives=new ArrayList<>();
    List<AssociatedUser> recruiters=new ArrayList<>();
    List<AssociatedUser> employees=new ArrayList<>();
    List<AssociatedUser> coordinators=new ArrayList<>();
    List<AssociatedUser> bdms=new ArrayList<>();

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public List<AssociatedUser> getSalesExecutives() {
        return salesExecutives;
    }

    public void setSalesExecutives(List<AssociatedUser> salesExecutives) {
        this.salesExecutives = salesExecutives;
    }

    public List<AssociatedUser> getRecruiters() {
        return recruiters;
    }

    public void setRecruiters(List<AssociatedUser> recruiters) {
        this.recruiters = recruiters;
    }

    public String getTeamLeadId() {
        return teamLeadId;
    }

    public void setTeamLeadId(String teamLeadId) {
        this.teamLeadId = teamLeadId;
    }

    public String getTeamLeadName() {
        return teamLeadName;
    }

    public void setTeamLeadName(String teamLeadName) {
        this.teamLeadName = teamLeadName;
    }

    public List<AssociatedUser> getEmployees() {
        return employees;
    }

    public void setEmployees(List<AssociatedUser> employees) {
        this.employees = employees;
    }

    public List<AssociatedUser> getCoordinators() {
        return coordinators;
    }

    public void setCoordinators(List<AssociatedUser> coordinators) {
        this.coordinators = coordinators;
    }

    public List<AssociatedUser> getBdms() {
        return bdms;
    }

    public void setBdms(List<AssociatedUser> bdms) {
        this.bdms = bdms;
    }
}
