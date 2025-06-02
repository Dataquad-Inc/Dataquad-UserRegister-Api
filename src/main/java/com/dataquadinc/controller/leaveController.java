package com.dataquadinc.controller;

import com.dataquadinc.dto.leaveCalenderDto;
import com.dataquadinc.model.LeaveCalender;
import com.dataquadinc.service.leaveCalenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/users")
public class leaveController {
    @Autowired
    private leaveCalenderService leaveCalendarService;

    @PostMapping("/save")
    public ResponseEntity<LeaveCalender> saveLeave(@RequestBody leaveCalenderDto dto) {
        LeaveCalender savedLeave = leaveCalendarService.saveLeave(dto);
        return ResponseEntity.ok(savedLeave);
    }

//    @PostMapping("/save")
//    public ResponseEntity<?> saveLeave(@RequestBody leaveCalenderDto dto) {
//        try {
//            LeaveCalender savedLeave = leaveCalendarService.saveLeave(dto);
//            return ResponseEntity.ok(savedLeave);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error saving leave: " + e.getMessage());
//        }
//    }

    @GetMapping("/leaves")
    public ResponseEntity<List<LeaveCalender>> getAllLeaves() {
        List<LeaveCalender> leaves = leaveCalendarService.getAllLeaves();
        System.out.println("hii");
        return ResponseEntity.ok(leaves);
    }
}
