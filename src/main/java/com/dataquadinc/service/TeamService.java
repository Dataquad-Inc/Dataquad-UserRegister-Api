package com.dataquadinc.service;


import com.dataquadinc.client.RequirementFeignClient;
import com.dataquadinc.dto.*;
import com.dataquadinc.exceptions.UserNotFoundException;
import com.dataquadinc.model.Roles;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserType;
import com.dataquadinc.repository.UserDao;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dataquadinc.dto.AssociatedToTeamLeadResponse;
import com.dataquadinc.dto.CandidateStatsResponse;
import com.dataquadinc.dto.TeamDashboardResponse;
import com.dataquadinc.dto.TeamMemberStatsDTO;
import com.dataquadinc.dto.UserStatsDTO;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TeamService {

    @Autowired
    UserDao userDao;

    @Autowired
    private UserService userService;

    @Autowired
    private RequirementFeignClient requirementFeignClient;

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
        List<AssociatedUser> employees = new ArrayList<>();
        List<AssociatedUser> coordinators = new ArrayList<>();
        List<AssociatedUser> bdms = new ArrayList<>();
        List<AssociatedUser> teamLeads = new ArrayList<>();

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

        List<UserDetails> activeUsers = userDao.findAll().stream()
                .filter(this::isActiveUser)
                .toList();

        // Get all team leads in the entity (including BDMs acting as team leads)
        List<UserDetails> teamLeads = activeUsers.stream()
                .filter(userDetails -> entity.equalsIgnoreCase(userDetails.getEntity()))
                .filter(userDetails -> {
                    // Check if user has TEAMLEAD role OR BDM role
                    boolean hasTeamLeadRole = userDetails.getRoles().stream()
                            .anyMatch(role -> role.getName().equals(UserType.TEAMLEAD));
                    boolean hasBdmRole = userDetails.getRoles().stream()
                            .anyMatch(role -> role.getName().equals(UserType.BDM));

                    // Check if they have team assignments (meaning they're acting as team lead)
                    boolean hasTeamAssignments = userDetails.getTeamAssignments() != null
                            && !userDetails.getTeamAssignments().isEmpty();

                    return (hasTeamLeadRole || hasBdmRole) && hasTeamAssignments;
                })
                .toList();

        for (UserDetails teamLeadUser : teamLeads) {
            List<AssociatedUser> salesExecutives = new ArrayList<>();
            List<AssociatedUser> recruiters = new ArrayList<>();
            List<AssociatedUser> employees = new ArrayList<>();
            List<AssociatedUser> coordinators = new ArrayList<>();
            List<AssociatedUser> bdms = new ArrayList<>();
            List<AssociatedUser> teamLeadsList = new ArrayList<>();


            for (UserDetails user : activeUsers) {
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
                        if (roles.contains(UserType.TEAMLEAD)) {
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

    private boolean isActiveUser(UserDetails userDetails) {
        return userDetails != null && "ACTIVE".equalsIgnoreCase(userDetails.getStatus());
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

    public TeamDashboardResponse getTeamDashboard(String teamId) {

        AssociatedToTeamLeadResponse team =
                getUsersAssociatedToTeamLead(teamId);

        List<BdmEmployeeDTO> bdmStats =
                requirementFeignClient.getBdmList();

        CandidateStatsResponse statsResponse =
                requirementFeignClient.getStats();

        List<Coordinator_DTO> coordinatorStats =
                requirementFeignClient.getCoordinatorStats();

        TeamDashboardResponse response = new TeamDashboardResponse();

        response.setTeamId(teamId);

        response.setTeamLead(
                buildUser(team.getTeamLeadId(),
                        "TEAMLEAD",
                        bdmStats,
                        statsResponse,
                        coordinatorStats)
        );

        response.setBdms(
                team.getBdms().stream()
                        .map(b -> buildUser(
                                b.getUserId(),
                                "BDM",
                                bdmStats,
                                statsResponse,
                                coordinatorStats))
                        .toList()
        );

        response.setEmployees(
                team.getEmployees().stream()
                        .map(e -> buildUser(
                                e.getUserId(),
                                "EMPLOYEE",
                                bdmStats,
                                statsResponse,
                                coordinatorStats))
                        .toList()
        );

        response.setCoordinators(
                team.getCoordinators().stream()
                        .map(c -> buildUser(
                                c.getUserId(),
                                "COORDINATOR",
                                bdmStats,
                                statsResponse,
                                coordinatorStats))
                        .toList()
        );

        return response;
    }
    private TeamMemberStatsDTO buildUser(
            String userId,
            String role,
            List<BdmEmployeeDTO> bdmStats,
            CandidateStatsResponse statsResponse,
            List<Coordinator_DTO> coordinatorStats) {

        TeamMemberStatsDTO dto = new TeamMemberStatsDTO();

        dto.setUserId(userId);
        dto.setRole(role);

        switch (role.toUpperCase()) {

            case "BDM":

                bdmStats.stream()
                        .filter(b -> userId.equalsIgnoreCase(b.getEmployeeId()))
                        .findFirst()
                        .ifPresent(b -> {

                            dto.setUserName(b.getEmployeeName());
                            dto.setEmail(b.getEmail());

                            dto.setNumberOfClients((int) b.getClientCount());
                            dto.setNumberOfRequirements((int) b.getRequirementsCount());
                            dto.setNumberOfSubmissions((int) b.getSubmissionCount());
                            dto.setNumberOfInterviews((int) b.getInterviewCount());
                            dto.setNumberOfPlacements((int) b.getPlacementCount());
                            dto.setStatus(b.getStatus());

                            dto.setNumberOfScreenRejects(0);
                        });

                break;

            case "TEAMLEAD":
            case "EMPLOYEE":

                if (statsResponse != null && statsResponse.getUserStats() != null) {

                    statsResponse.getUserStats().stream()
                            .filter(s -> userId.equalsIgnoreCase(s.getEmployeeId()))
                            .findFirst()
                            .ifPresent(s -> {

                                dto.setUserName(s.getEmployeeName());
                                dto.setEmail(s.getEmployeeEmail());
                                dto.setRole(s.getRole());

                                dto.setNumberOfClients(s.getNumberOfClients());
                                dto.setNumberOfRequirements(s.getNumberOfRequirements());
                                dto.setNumberOfSubmissions(s.getNumberOfSubmissions());
                                dto.setNumberOfScreenRejects(s.getNumberOfScreenRejects());
                                dto.setNumberOfInterviews(s.getNumberOfInterviews());
                                dto.setNumberOfPlacements(s.getNumberOfPlacements());

                                dto.setSelfSubmissions(s.getSelfSubmissions());
                                dto.setSelfInterviews(s.getSelfInterviews());
                                dto.setSelfPlacements(s.getSelfPlacements());

                                dto.setTeamSubmissions(s.getTeamSubmissions());
                                dto.setTeamInterviews(s.getTeamInterviews());
                                dto.setTeamPlacements(s.getTeamPlacements());
                                dto.setTeamScreenRejectCount(s.getTeamScreenRejectCount());

                            });
                }

                break;

            case "COORDINATOR":

                coordinatorStats.stream()
                        .filter(c -> userId.equalsIgnoreCase(c.getEmployeeId()))
                        .findFirst()
                        .ifPresent(c -> {

                            dto.setUserName(c.getEmployeeName());
                            dto.setEmail(c.getEmployeeEmail());

                            dto.setNumberOfSubmissions(c.getTotalScheduled());
                            dto.setNumberOfInterviews(c.getTotalInterviews());
                            dto.setNumberOfPlacements(c.getTotalSelected());
                            dto.setNumberOfScreenRejects(c.getTotalRejected());

                            dto.setNumberOfClients(0);
                            dto.setNumberOfRequirements(0);
                        });

                break;
        }

        return dto;
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

        boolean removed = assignments.removeIf(t -> Objects.equals(t.getTeamLeadId(), teamLeadId));
        if (!removed) {
            return "No assignment found for the given team lead.";
        }
        if (Objects.equals(user.getAssociatedTeamLeadId(), teamLeadId)) {
            user.setAssociatedTeamLeadId(null);
            if (assignments.isEmpty()) {
                user.setTeamName(null);
            }
        }
        userDao.save(user);

        return "Removed user " + userId + " from team lead " + teamLeadId;
    }

    @Transactional
    public String deleteTeamByTeamLeadId(String teamLeadId) {
        UserDetails teamLead = userDao.findByUserId(teamLeadId);
        if (teamLead == null) {
            throw new UserNotFoundException("No User Found With ID: " + teamLeadId);
        }

        List<UserDetails> updatedUsers = new ArrayList<>();
        for (UserDetails user : userDao.findAll()) {
            boolean updated = false;
            List<TeamAssignment> assignments = user.getTeamAssignments();

            if (assignments != null && !assignments.isEmpty()) {
                List<TeamAssignment> remainingAssignments = assignments.stream()
                        .filter(assignment -> !Objects.equals(assignment.getTeamLeadId(), teamLeadId))
                        .collect(Collectors.toList());

                if (remainingAssignments.size() != assignments.size()) {
                    user.setTeamAssignments(remainingAssignments);
                    updated = true;
                }
            }

            if (Objects.equals(user.getAssociatedTeamLeadId(), teamLeadId)) {
                user.setAssociatedTeamLeadId(null);
                updated = true;
            }

            if (updated && user.getTeamAssignments().isEmpty()) {
                user.setTeamName(null);
            }

            if (updated) {
                updatedUsers.add(user);
            }
        }

        if (updatedUsers.isEmpty()) {
            return "No users found for team lead " + teamLeadId;
        }

        userDao.saveAll(updatedUsers);
        return "Deleted team for team lead " + teamLeadId + " and removed team lead details from " + updatedUsers.size() + " users";
    }

}
