package com.dataquadinc.dto;

import java.util.List;
import java.util.Set;

public class AssignTeamLead {

    private String superAdmin;

    private String teamName;

    private String teamLead;

    private Set<String> salesExecutives;

    private Set<String> recruiters;

    private Set<String> bdms;

    private Set<String> employees;

    private Set<String> coordinators;

    private Set<String> teamLeads;

    public String getSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(String superAdmin) {
        this.superAdmin = superAdmin;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamLead() {
        return teamLead;
    }

    public void setTeamLead(String teamLead) {
        this.teamLead = teamLead;
    }

    public Set<String> getSalesExecutives() {
        return salesExecutives;
    }

    public void setSalesExecutives(Set<String> salesExecutives) {
        this.salesExecutives = salesExecutives;
    }

    public Set<String> getRecruiters() {
        return recruiters;
    }

    public void setRecruiters(Set<String> recruiters) {
        this.recruiters = recruiters;
    }

    public Set<String> getBdms() {
        return bdms;
    }

    public void setBdms(Set<String> bdms) {
        this.bdms = bdms;
    }

    public Set<String> getEmployees() {
        return employees;
    }

    public void setEmployees(Set<String> employees) {
        this.employees = employees;
    }

    public Set<String> getCoordinators() {
        return coordinators;
    }

    public void setCoordinators(Set<String> coordinators) {
        this.coordinators = coordinators;
    }

    public Set<String> getTeamLeads() {
        return teamLeads;
    }

    public void setTeamLeads(Set<String> teamLeads) {
        this.teamLeads = teamLeads;
    }
}
