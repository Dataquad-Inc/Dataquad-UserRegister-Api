package com.dataquadinc.controller;

import com.dataquadinc.dto.AssignTeamLead;
import com.dataquadinc.dto.AssociatedToTeamLeadResponse;
import com.dataquadinc.dto.AssociatedUser;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com","http://localhost:3000","http://192.168.0.135:8080","http://192.168.0.135",
        "http://154.210.288.26",
        "http://192.168.0.203:3000",
        "http://192.168.0.167:3000"})
@RestController
@RequestMapping("/users")
public class TeamController {

    @Autowired
    TeamService teamService;

    @PostMapping("/assignTeamLead/{userId}")
    public ResponseEntity<String> assignTeamLead(@PathVariable String userId,
                                                 @RequestBody AssignTeamLead assignTeamLeadDto){

       return new ResponseEntity<>(teamService.assignTeamLead(userId,assignTeamLeadDto), HttpStatus.CREATED);

    }

    @GetMapping("/associated-users/{teamLeadId}")
    public ResponseEntity<AssociatedToTeamLeadResponse> getUsersAssociatedToTeamLead(@PathVariable String teamLeadId){

       return new ResponseEntity<>(teamService.getUsersAssociatedToTeamLead(teamLeadId),HttpStatus.OK);
    }
    @GetMapping("/AllAssociatedUsers")
    public ResponseEntity<?> getAllUsersAssociatedToTeamLead(
            @RequestParam(defaultValue = "US") String entity,
            @RequestParam(required = false) String userId
    ){
        if (userId != null && !userId.isEmpty()) {
            String teamLeadId = teamService.getTeamLeadIdByUserId(userId);
            return ResponseEntity.ok(teamLeadId);
        }
        return new ResponseEntity<>(teamService.getAllUsersAssociatedToTeamLead(entity), HttpStatus.OK);
    }


    @DeleteMapping("/team/{teamLeadId}/user/{userId}")
    public String removeUser(@PathVariable String teamLeadId, @PathVariable String userId) {
        return teamService.removeUserFromTeamLead(userId, teamLeadId);
    }

    @DeleteMapping("/team/{teamLeadId}")
    public String deleteTeam(@PathVariable String teamLeadId) {
        return teamService.deleteTeamByTeamLeadId(teamLeadId);
    }

}
