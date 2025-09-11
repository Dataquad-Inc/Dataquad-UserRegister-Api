package com.dataquadinc.service;

import com.dataquadinc.dto.AssignTeamLead;
import com.dataquadinc.dto.AssociatedToTeamLeadResponse;
import com.dataquadinc.dto.AssociatedUser;
import com.dataquadinc.dto.TeamAssignment;
import com.dataquadinc.exceptions.UserNotFoundException;
import com.dataquadinc.model.Roles;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserType;
import com.dataquadinc.repository.UserDao;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TeamService {

    @Autowired
    UserDao userDao;

    @Transactional
    public String assignTeamLead(String userId, AssignTeamLead assignTeamLeadDto) {

        UserDetails userDetails = userDao.findByUserId(userId);
        if (userDetails == null) {
            throw new UserNotFoundException("No User Found With Id :" + userId);
        }

        boolean isSuperAdmin = userDetails.getRoles().stream()
                .anyMatch(role -> role.getName() == UserType.SUPERADMIN);

        UserDetails teamLeadUser = userDao.findByUserId(assignTeamLeadDto.getTeamLead());
        if (teamLeadUser == null) {
            throw new UserNotFoundException("No User Found with ID :" + assignTeamLeadDto.getTeamLead());
        }

        boolean isTeamLead = teamLeadUser.getRoles().stream()
                .anyMatch(role -> role.getName() == UserType.TEAMLEAD);

        if (!isTeamLead) {
            log.error("User {} Not A TEAMLEAD", assignTeamLeadDto.getTeamLead());
            throw new UserNotFoundException("User " + teamLeadUser.getUserId() + " Not A TEAMLEAD");
        }

        // SuperAdmin assigning teamLead
        if (isSuperAdmin && isTeamLead) {
            teamLeadUser.addTeamAssignmentIfNotExists(
                    new TeamAssignment(userId, assignTeamLeadDto.getTeamName())
            );
            userDao.save(teamLeadUser);
        }

        //Assign Sales Executives
        if (!assignTeamLeadDto.getSalesExecutives().isEmpty()) {
            for (String salesExecutiveId : assignTeamLeadDto.getSalesExecutives()) {
                UserDetails salesUser = userDao.findByUserId(salesExecutiveId);
                if (salesUser == null) {
                    throw new UserNotFoundException("No User Found With ID " + salesExecutiveId);
                }

                boolean isSalesExecutive = salesUser.getRoles().stream()
                        .anyMatch(role -> role.getName() == UserType.SALESEXECUTIVE);
                if (!isSalesExecutive) {
                    throw new UserNotFoundException("User " + salesUser.getUserId() + " Not A SALESEXECUTIVE");
                }

                salesUser.addTeamAssignmentIfNotExists(
                        new TeamAssignment(assignTeamLeadDto.getTeamLead(), assignTeamLeadDto.getTeamName())
                );

                userDao.save(salesUser);
            }
        }

        // Assign Recruiters
        if (!assignTeamLeadDto.getRecruiters().isEmpty()) {
            for (String recruiterId : assignTeamLeadDto.getRecruiters()) {
                UserDetails recruiterUser = userDao.findByUserId(recruiterId);
                if (recruiterUser == null) {
                    throw new UserNotFoundException("No User Found With ID " + recruiterId);
                }

                boolean isRecruiter = recruiterUser.getRoles().stream()
                        .anyMatch(role -> role.getName() == UserType.RECRUITER);
                if (!isRecruiter) {
                    throw new UserNotFoundException("User " + recruiterUser.getUserId() + " Not A RECRUITER");
                }

                recruiterUser.addTeamAssignmentIfNotExists(
                        new TeamAssignment(assignTeamLeadDto.getTeamLead(), assignTeamLeadDto.getTeamName())
                );

                userDao.save(recruiterUser);
            }
        }
        return "Assigned Recruiters and SalesExecutives to TEAMLEAD " + assignTeamLeadDto.getTeamLead();
    }


    public AssociatedToTeamLeadResponse getUsersAssociatedToTeamLead(String teamLeadId) {
        List<AssociatedUser> salesExecutives = new ArrayList<>();
        List<AssociatedUser> recruiters = new ArrayList<>();

        // Validate team lead exists
        UserDetails teamLead = userDao.findByUserId(teamLeadId);
        if (teamLead == null) {
            throw new UserNotFoundException("No User Found With ID " + teamLeadId);
        }

        // Iterate all users and check if they belong to this team lead
        List<UserDetails> allUsers = userDao.findAll();
        for (UserDetails user : allUsers) {
            if (user.getTeamAssignments() != null) {
                boolean assignedToThisLead = user.getTeamAssignments().stream()
                        .anyMatch(t -> t.getTeamLeadId().equals(teamLeadId));
                if (assignedToThisLead) {
                    Set<UserType> roles = user.getRoles().stream()
                            .map(Roles::getName)
                            .collect(Collectors.toSet());

                    if (roles.contains(UserType.SALESEXECUTIVE)) {
                        salesExecutives.add(new AssociatedUser(user.getUserId(), user.getUserName()));
                    }
                    if (roles.contains(UserType.RECRUITER)) {
                        recruiters.add(new AssociatedUser(user.getUserId(), user.getUserName()));
                    }
                }
            }
        }

        AssociatedToTeamLeadResponse response = new AssociatedToTeamLeadResponse();
        response.setTeamLeadId(teamLead.getUserId());
        response.setTeamLeadName(teamLead.getUserName());

        response.setTeamName(teamLead.getTeamName());
        response.setRecruiters(recruiters);
        response.setSalesExecutives(salesExecutives);
        return response;
    }

    public List<AssociatedToTeamLeadResponse> getAllUsersAssociatedToTeamLead() {
        List<AssociatedToTeamLeadResponse> result = new ArrayList<>();

        // Get all team leads in US entity
        List<UserDetails> teamLeads = userDao.findAll().stream()
                .filter(userDetails -> "US".equalsIgnoreCase(userDetails.getEntity()))
                .filter(userDetails -> userDetails.getRoles().stream()
                        .anyMatch(role -> role.getName().equals(UserType.TEAMLEAD)))
                .toList();

        List<UserDetails> allUsers = userDao.findAll();

        for (UserDetails teamLeadUser : teamLeads) {
            List<AssociatedUser> salesExecutives = new ArrayList<>();
            List<AssociatedUser> recruiters = new ArrayList<>();

            for (UserDetails user : allUsers) {
                if (user.getTeamAssignments() != null) {
                    boolean assignedToThisLead = user.getTeamAssignments().stream()
                            .anyMatch(t -> t.getTeamLeadId().equals(teamLeadUser.getUserId()));
                    if (assignedToThisLead) {
                        Set<UserType> roles = user.getRoles().stream()
                                .map(Roles::getName)
                                .collect(Collectors.toSet());

                        if (roles.contains(UserType.SALESEXECUTIVE)) {
                            salesExecutives.add(new AssociatedUser(user.getUserId(), user.getUserName()));
                        }
                        if (roles.contains(UserType.RECRUITER)) {
                            recruiters.add(new AssociatedUser(user.getUserId(), user.getUserName()));
                        }
                    }
                }
            }

            AssociatedToTeamLeadResponse response = new AssociatedToTeamLeadResponse();
            response.setTeamLeadId(teamLeadUser.getUserId());
            response.setTeamLeadName(teamLeadUser.getUserName());

            response.setTeamName(teamLeadUser.getTeamName());
            response.setRecruiters(recruiters);
            response.setSalesExecutives(salesExecutives);

            result.add(response);
        }
        return result;
    }
    @Transactional
    public String removeUserFromTeamLead(String userId, String teamLeadId) {

        UserDetails user = userDao.findByUserId(userId);
        if (user == null) {
            throw new UserNotFoundException("No User Found With ID: " + userId);
        }

        List<TeamAssignment> assignments = user.getTeamAssignments();
        if (assignments == null || assignments.isEmpty()) {
            throw new UserNotFoundException("User Not Assigned to Team");
        }

        boolean removed = assignments.removeIf(t -> t.getTeamLeadId().equals(teamLeadId));
        if (!removed) {
            return "No assignment found for the given team lead.";
        }
        userDao.save(user);

        return "Removed user " + userId + " from team lead " + teamLeadId;
    }

}
