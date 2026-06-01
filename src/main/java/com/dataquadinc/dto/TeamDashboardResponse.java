package com.dataquadinc.dto;

import java.util.List;

public class TeamDashboardResponse {
    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public TeamMemberStatsDTO getTeamLead() {
        return teamLead;
    }

    public void setTeamLead(TeamMemberStatsDTO teamLead) {
        this.teamLead = teamLead;
    }

    public List<TeamMemberStatsDTO> getBdms() {
        return bdms;
    }

    public void setBdms(List<TeamMemberStatsDTO> bdms) {
        this.bdms = bdms;
    }

    public List<TeamMemberStatsDTO> getEmployees() {
        return employees;
    }

    public void setEmployees(List<TeamMemberStatsDTO> employees) {
        this.employees = employees;
    }

    public List<TeamMemberStatsDTO> getCoordinators() {
        return coordinators;
    }

    public void setCoordinators(List<TeamMemberStatsDTO> coordinators) {
        this.coordinators = coordinators;
    }

    private String teamId;

    private TeamMemberStatsDTO teamLead;
    private List<TeamMemberStatsDTO> bdms;
    private List<TeamMemberStatsDTO> employees;
    private List<TeamMemberStatsDTO> coordinators;

    // getters/setters
}