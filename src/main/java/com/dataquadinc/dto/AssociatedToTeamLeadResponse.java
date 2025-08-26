package com.dataquadinc.dto;

import java.util.ArrayList;
import java.util.List;

public class AssociatedToTeamLeadResponse {

    private String teamName;
    private String teamLeadId;
    private String teamLeadName;
    List<AssociatedUser> salesExecutives=new ArrayList<>();
    List<AssociatedUser> recruiters=new ArrayList<>();

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
}
