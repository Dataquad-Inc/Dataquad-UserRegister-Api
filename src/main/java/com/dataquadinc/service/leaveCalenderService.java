package com.dataquadinc.service;

import com.dataquadinc.dto.leaveCalenderDto;
import com.dataquadinc.model.LeaveCalender;
import com.dataquadinc.repository.leaveCalendarDao;
import org.hibernate.mapping.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class leaveCalenderService {

    @Autowired
    private leaveCalendarDao leaveCalendarDaoo;

    @Autowired
    private EmailService emailService;  // Inject the EmailService

    public LeaveCalender saveLeave(leaveCalenderDto dto) {
        LeaveCalender leave = new LeaveCalender();
        leave.setUserId(dto.getUserId());
       leave.setManagerEmail(dto.getManagerEmail());
       leave.setUserName(dto.getUserName());
        leave.setDescription(dto.getDescription());
        leave.setStartDate(dto.getStartDate());
        leave.setEndDate(dto.getEndDate());
        leave.setNoOfDays(dto.getNoOfDays());
        leave.setLeaveType(dto.getLeaveType());


        // Save the leave
        LeaveCalender savedLeave = leaveCalendarDaoo.save(leave);

// Send email to manager after leave is saved
        String[] managerEmailArray = dto.getManagerEmail().toArray(new String[0]);// Assuming you have the manager's email in the DTO
        String subject = "Leave Request Notification";
        String body = "Respected Manager,\n\n" +
                "I am writing to formally request leave for the following period:\n\n" +
                "Start Date: " + dto.getStartDate() + "\n" +
                "End Date: " + dto.getEndDate() + "\n" +
                "Number of Days: " + dto.getNoOfDays() + "\n" +
                "Leave Type: " + dto.getLeaveType() + "\n\n" +
                "Reason for Leave:\n" +
                dto.getDescription() + "\n\n" +
                "User ID: " + dto.getUserId() + "\n"+
                "User name:"+dto.getUserName();

// Send the email
//        emailService.sendEmail(managerEmailArray, subject, body);

        return savedLeave;
    }
    public List<LeaveCalender> getAllLeaves() {
        return leaveCalendarDaoo.findAll(); // Fetch all leave records
    }
}
