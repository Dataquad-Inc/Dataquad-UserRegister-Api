package com.dataquadinc.service;

import com.dataquadinc.dto.AssignTeamLead;
import com.dataquadinc.dto.AssociatedToTeamLeadResponse;
import com.dataquadinc.dto.AssociatedUser;
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
    public String assignTeamLead(String userId,AssignTeamLead assignTeamLeadDto){

            UserDetails userDetails = userDao.findByUserId(userId);
            if (userDetails == null) throw new UserNotFoundException("No User Found With Id :" + userId);
            boolean isSuperAdmin = userDetails.getRoles().stream()
                    .anyMatch(role -> role.getName() == UserType.SUPERADMIN);
        UserDetails teamLeadUser=userDao.findByUserId(assignTeamLeadDto.getTeamLead());
        if(teamLeadUser==null) throw new UserNotFoundException("No User Found with ID :"+assignTeamLeadDto.getTeamLead());
        boolean isTeamLead = teamLeadUser.getRoles().stream()
                .anyMatch(role -> role.getName() == UserType.TEAMLEAD);
        if(!isTeamLead){
            log.error("No User Found with ID :"+assignTeamLeadDto.getTeamLead());
            throw new UserNotFoundException("User "+teamLeadUser.getUserId()+" Not A TEAMLEAD");
        }
        if(isSuperAdmin && isTeamLead){
             teamLeadUser.setAssociatedTeamLeadId(userId);
             teamLeadUser.setTeamName(assignTeamLeadDto.getTeamName());
        }
        if(!assignTeamLeadDto.getSalesExecutives().isEmpty()) {
            for (String salesExecutiveId : assignTeamLeadDto.getSalesExecutives()) {
                UserDetails salesUser = userDao.findByUserId(salesExecutiveId);
                if (salesUser == null)
                    throw new UserNotFoundException("No User Found With ID " + salesUser.getUserId());
                boolean isSalesExecutive = salesUser.getRoles().stream()
                        .anyMatch(role -> role.getName() == UserType.SALESEXECUTIVE);
                if(!isSalesExecutive){
                    log.error("No User Found with ID :"+assignTeamLeadDto.getTeamLead());
                    throw new UserNotFoundException("User "+teamLeadUser.getUserId()+" Not A SALESEXECUTIVE");
                }
                salesUser.setAssociatedTeamLeadId(assignTeamLeadDto.getTeamLead());
                salesUser.setTeamName(assignTeamLeadDto.getTeamName());
                userDao.save(salesUser);
            }
        }
        if (!assignTeamLeadDto.getRecruiters().isEmpty()) {
            for (String recruiterId : assignTeamLeadDto.getRecruiters()) {
                UserDetails recruiterUser = userDao.findByUserId(recruiterId);
                if (recruiterUser == null)
                    throw new UserNotFoundException("No User Found With ID " + recruiterUser.getUserId());
                boolean isSalesExecutive = recruiterUser.getRoles().stream()
                        .anyMatch(role -> role.getName() == UserType.RECRUITER);
                if(!isSalesExecutive){
                    log.error("No User Found with ID :"+assignTeamLeadDto.getTeamLead());
                    throw new UserNotFoundException("User "+teamLeadUser.getUserId()+" Not A RECRUITER");
                }
                recruiterUser.setAssociatedTeamLeadId(assignTeamLeadDto.getTeamLead());
                recruiterUser.setTeamName(assignTeamLeadDto.getTeamName());
                userDao.save(recruiterUser);
            }
        }

        return "Assigned Recruiters And SalesExecutives To TEAMLEAD "+assignTeamLeadDto.getTeamLead();
    }

    public AssociatedToTeamLeadResponse getUsersAssociatedToTeamLead(String teamLeadId){

        List<AssociatedUser> salesExecutives=new ArrayList<>();
        List<AssociatedUser> recruiters=new ArrayList<>();
        UserDetails teamlead=userDao.findByUserId(teamLeadId);
        if(teamlead==null){
            throw new UserNotFoundException("No User Found With ID "+teamLeadId);
        }
        String teamName=teamlead.getTeamName();
        List<UserDetails> associatedUsers=userDao.findByAssociatedTeamLeadId(teamLeadId);

       for (UserDetails user:associatedUsers){
          Set<UserType> roles=user.getRoles().stream().map(Roles::getName)
                   .collect(Collectors.toSet());

           if (roles.contains(UserType.SALESEXECUTIVE))   salesExecutives.add(new AssociatedUser(user.getUserId(), user.getUserName()));
           if(roles.contains(UserType.RECRUITER))   recruiters.add(new AssociatedUser(user.getUserId(), user.getUserName()));

       }
        AssociatedToTeamLeadResponse response=new AssociatedToTeamLeadResponse();
        response.setTeamName(teamName);
        response.setTeamLeadId(teamlead.getUserId());
        response.setTeamLeadName(teamlead.getUserName());
        response.setRecruiters(recruiters);
        response.setSalesExecutives(salesExecutives);
        return response;
    }
    public List<AssociatedToTeamLeadResponse> getAllUsersAssociatedToTeamLead(){

        List<AssociatedToTeamLeadResponse> result=new ArrayList<>();

       List<String> teamLeads=userDao.findAll().
               stream().
               filter(userDetails -> userDetails.getEntity().equalsIgnoreCase("US")).
               filter(userDetails -> userDetails.getRoles().
                       stream().anyMatch(roles -> roles.getName().equals(UserType.TEAMLEAD)))
               .map(UserDetails::getUserId)
               .collect(Collectors.toList());

       for(String teamLead:teamLeads){
          UserDetails teamLeadUser=userDao.findByUserId(teamLead);
           String teamName=teamLeadUser.getTeamName();
           List<UserDetails> associatedUsers=userDao.findByAssociatedTeamLeadId(teamLead);
           List<AssociatedUser> salesExecutives=new ArrayList<>();
           List<AssociatedUser> recruiters=new ArrayList<>();
           for (UserDetails user:associatedUsers){
               Set<UserType> roles=user.getRoles().stream().map(Roles::getName)
                       .collect(Collectors.toSet());
               if (roles.contains(UserType.SALESEXECUTIVE))   salesExecutives.add(new AssociatedUser(user.getUserId(), user.getUserName()));
               if(roles.contains(UserType.RECRUITER))   recruiters.add(new AssociatedUser(user.getUserId(), user.getUserName()));

           }
           AssociatedToTeamLeadResponse response=new AssociatedToTeamLeadResponse();
           response.setTeamName(teamName);
           response.setTeamLeadId(teamLeadUser.getUserId());
           response.setTeamLeadName(teamLeadUser.getUserName());
           response.setRecruiters(recruiters);
           response.setSalesExecutives(salesExecutives);
           result.add(response);
           recruiters=new ArrayList<>();
           salesExecutives=new ArrayList<>();
       }
             return result;
    }
}
