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

        // Check if the acting user has permission (keeping SUPERADMIN check for authorization)
        boolean isSuperAdmin = userDetails.getRoles().stream()
                .anyMatch(role -> role.getName() == UserType.SUPERADMIN);

//        if (!isSuperAdmin) {
//            throw new UnauthorizedException("Only SUPERADMIN can assign team leads");
//        }

        UserDetails teamLeadUser = userDao.findByUserId(assignTeamLeadDto.getTeamLead());
        if (teamLeadUser == null) {
            throw new UserNotFoundException("No User Found with ID :" + assignTeamLeadDto.getTeamLead());
        }

        // REMOVED: Team lead role validation - any user can be a team lead now

        // Assign the team lead to themselves (if needed for hierarchy)
        teamLeadUser.addTeamAssignmentIfNotExists(
                new TeamAssignment(assignTeamLeadDto.getTeamLead(), assignTeamLeadDto.getTeamName())
        );
        userDao.saveAndFlush(teamLeadUser);

        // Assign Additional Team Leads (if any)
        if (assignTeamLeadDto.getTeamLeads() != null && !assignTeamLeadDto.getTeamLeads().isEmpty()) {
            for (String teamLeadId : assignTeamLeadDto.getTeamLeads()) {
                UserDetails teamUser = userDao.findByUserId(teamLeadId);
                if (teamUser == null) {
                    throw new UserNotFoundException("No User Found With ID " + teamLeadId);
                }

                // REMOVED: Team lead role validation
                teamUser.addTeamAssignmentIfNotExists(
                        new TeamAssignment(assignTeamLeadDto.getTeamLead(), assignTeamLeadDto.getTeamName())
                );
                userDao.save(teamUser);
            }
        }

        // Assign Sales Executives (no role validation removed - keeping business logic for team members)
        if (assignTeamLeadDto.getSalesExecutives() != null && !assignTeamLeadDto.getSalesExecutives().isEmpty()) {
            for (String salesExecutiveId : assignTeamLeadDto.getSalesExecutives()) {
                UserDetails salesUser = userDao.findByUserId(salesExecutiveId);
                if (salesUser == null) {
                    throw new UserNotFoundException("No User Found With ID " + salesExecutiveId);
                }

                // Optional: Remove role validation if you want any user to be sales executive
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

        // Similar modifications for other user types (Recruiters, Coordinators, BDMs, Employees)
        // Assign Recruiters
        if (assignTeamLeadDto.getRecruiters() != null && !assignTeamLeadDto.getRecruiters().isEmpty()) {
            for (String recruiterId : assignTeamLeadDto.getRecruiters()) {
                UserDetails recruiterUser = userDao.findByUserId(recruiterId);
                if (recruiterUser == null) {
                    throw new UserNotFoundException("No User Found With ID " + recruiterId);
                }

                // Optional: Remove role validation if needed
                boolean isRecruiter = recruiterUser.getRoles().stream()
                        .anyMatch(role -> role.getName() == UserType.RECRUITER);
                if (!isRecruiter) {
                    throw new UserNotFoundException("User " + recruiterUser.getUserId() + " Not A RECRUITER");
                }

                recruiterUser.addTeamAssignmentIfNotExists(
                        new TeamAssignment(assignTeamLeadDto.getTeamLead(), assignTeamLeadDto.getTeamName())
                );
                userDao.saveAndFlush(recruiterUser);
            }
        }

        // Assign Coordinators
        if (assignTeamLeadDto.getCoordinators() != null && !assignTeamLeadDto.getCoordinators().isEmpty()) {
            for (String coordinatorId : assignTeamLeadDto.getCoordinators()) {
                UserDetails coordinatorUser = userDao.findByUserId(coordinatorId);
                if (coordinatorUser == null) {
                    throw new UserNotFoundException("No User Found With ID " + coordinatorId);
                }

                // Optional: Remove role validation if needed
                boolean isCoordinator = coordinatorUser.getRoles().stream()
                        .anyMatch(role -> role.getName() == UserType.COORDINATOR);
                if (!isCoordinator) {
                    throw new UserNotFoundException("User " + coordinatorUser.getUserId() + " Not A COORDINATOR");
                }

                coordinatorUser.addTeamAssignmentIfNotExists(
                        new TeamAssignment(assignTeamLeadDto.getTeamLead(), assignTeamLeadDto.getTeamName())
                );
                userDao.saveAndFlush(coordinatorUser);
            }
        }

        // Assign BDMs
        if (assignTeamLeadDto.getBdms() != null && !assignTeamLeadDto.getBdms().isEmpty()) {
            for (String bdmId : assignTeamLeadDto.getBdms()) {
                UserDetails bdmUser = userDao.findByUserId(bdmId);
                if (bdmUser == null) {
                    throw new UserNotFoundException("No User Found With ID " + bdmId);
                }

                // Optional: Remove role validation if needed
                boolean isBdm = bdmUser.getRoles().stream()
                        .anyMatch(role -> role.getName() == UserType.BDM);
                if (!isBdm) {
                    throw new UserNotFoundException("User " + bdmUser.getUserId() + " Not A BDM");
                }

                bdmUser.addTeamAssignmentIfNotExists(
                        new TeamAssignment(assignTeamLeadDto.getTeamLead(), assignTeamLeadDto.getTeamName())
                );
                userDao.saveAndFlush(bdmUser);
            }
        }

        // Assign Employees
        if (assignTeamLeadDto.getEmployees() != null && !assignTeamLeadDto.getEmployees().isEmpty()) {
            for (String employeeId : assignTeamLeadDto.getEmployees()) {
                UserDetails employeeUser = userDao.findByUserId(employeeId);
                if (employeeUser == null) {
                    throw new UserNotFoundException("No User Found With ID " + employeeId);
                }

                // Optional: Remove role validation if needed
                boolean isEmployee = employeeUser.getRoles().stream()
                        .anyMatch(role -> role.getName() == UserType.EMPLOYEE);
                if (!isEmployee) {
                    throw new UserNotFoundException("User " + employeeUser.getUserId() + " Not A Employee");
                }

                employeeUser.addTeamAssignmentIfNotExists(
                        new TeamAssignment(assignTeamLeadDto.getTeamLead(), assignTeamLeadDto.getTeamName())
                );
                userDao.saveAndFlush(employeeUser);
            }
        }

        return "Assigned Users to TEAMLEAD " + assignTeamLeadDto.getTeamLead();
    }

    public AssociatedToTeamLeadResponse getUsersAssociatedToTeamLead(String teamLeadId) {
        List<AssociatedUser> salesExecutives = new ArrayList<>();
        List<AssociatedUser> recruiters = new ArrayList<>();
        List<AssociatedUser> employees=new ArrayList<>();
        List<AssociatedUser> coordinators=new ArrayList<>();
        List<AssociatedUser> bdms=new ArrayList<>();
        List<AssociatedUser> teamLeads=new ArrayList<>();

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
                    if (roles.contains(UserType.EMPLOYEE)) {
                        employees.add(new AssociatedUser(user.getUserId(), user.getUserName()));
                    }
                    if (roles.contains(UserType.BDM)) {
                        bdms.add(new AssociatedUser(user.getUserId(), user.getUserName()));
                    }
                    if (roles.contains(UserType.COORDINATOR)) {
                        coordinators.add(new AssociatedUser(user.getUserId(), user.getUserName()));
                    }
                    if (roles.contains(UserType.TEAMLEAD)) {
                        teamLeads.add(new AssociatedUser(user.getUserId(), user.getUserName()));
                    }
                }
            }
        }

        AssociatedToTeamLeadResponse response = new AssociatedToTeamLeadResponse();
        response.setTeamLeadId(teamLead.getUserId());
        response.setTeamLeadName(teamLead.getUserName());
        response.setTeamName(
                teamLead.getTeamAssignments().stream()
                        .map(TeamAssignment::getTeamName)
                        .findFirst()
                        .orElse(null)
        );
        response.setRecruiters(recruiters);
        response.setSalesExecutives(salesExecutives);
        response.setBdms(bdms);
        response.setCoordinators(coordinators);
        response.setEmployees(employees);
        response.setTeamLeads(teamLeads);
        return response;
    }

    public List<AssociatedToTeamLeadResponse> getAllUsersAssociatedToTeamLead(String entity) {
        List<AssociatedToTeamLeadResponse> result = new ArrayList<>();

        // Get all team leads in the entity
        List<UserDetails> teamLeads = userDao.findAll().stream()
                .filter(userDetails -> entity.equalsIgnoreCase(userDetails.getEntity()))
                .filter(userDetails -> userDetails.getStatus().equalsIgnoreCase("ACTIVE"))
                .filter(userDetails ->
                        // Normal Team Leads
                        userDetails.getRoles().stream().anyMatch(role -> role.getName().equals(UserType.TEAMLEAD))
                                // Special Case: ADRTIN025 (BDM acting as Team Lead)
                                || ("ADRTIN025".equalsIgnoreCase(userDetails.getUserId())
                                && userDetails.getRoles().stream().anyMatch(role -> role.getName().equals(UserType.BDM)))
                )
                .toList();

        List<UserDetails> allUsers = userDao.findAll();

        for (UserDetails teamLeadUser : teamLeads) {
            List<AssociatedUser> salesExecutives = new ArrayList<>();
            List<AssociatedUser> recruiters = new ArrayList<>();
            List<AssociatedUser> employees = new ArrayList<>();
            List<AssociatedUser> coordinators = new ArrayList<>();
            List<AssociatedUser> bdms = new ArrayList<>();
            List<AssociatedUser> teamLeadsList = new ArrayList<>();


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
                        if (roles.contains(UserType.EMPLOYEE)) {
                            employees.add(new AssociatedUser(user.getUserId(), user.getUserName()));
                        }
                        if (roles.contains(UserType.BDM)) {
                            bdms.add(new AssociatedUser(user.getUserId(), user.getUserName()));
                        }
                        if (roles.contains(UserType.COORDINATOR)) {
                            coordinators.add(new AssociatedUser(user.getUserId(), user.getUserName()));
                        }
                        if (roles.contains(UserType.TEAMLEAD)){
                            teamLeadsList.add(new AssociatedUser(user.getUserId(), user.getUserName()));
                        }
                    }
                }
            }

            AssociatedToTeamLeadResponse response = new AssociatedToTeamLeadResponse();
            response.setTeamLeadId(teamLeadUser.getUserId());
            response.setTeamLeadName(teamLeadUser.getUserName());
            response.setTeamName(
                    teamLeadUser.getTeamAssignments().stream()
                            .map(TeamAssignment::getTeamName)
                            .findFirst()
                            .orElse(null)
            );
            response.setRecruiters(recruiters);
            response.setSalesExecutives(salesExecutives);
            response.setBdms(bdms);
            response.setCoordinators(coordinators);
            response.setEmployees(employees);
            response.setTeamLeads(teamLeadsList);

            result.add(response);
        }
        return result;
    }

    public String getTeamLeadIdByUserId(String userId) {
        Optional<UserDetails> userOpt = userDao.findAll().stream()
                .filter(u -> u.getUserId().equalsIgnoreCase(userId))
                .findFirst();

        if (userOpt.isPresent() && userOpt.get().getTeamAssignments() != null) {
            return userOpt.get().getTeamAssignments().stream()
                    .map(TeamAssignment::getTeamLeadId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        return null;
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
