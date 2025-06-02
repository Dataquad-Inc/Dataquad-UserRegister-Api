package com.dataquadinc.controller;



import com.dataquadinc.model.Timesheet;
import com.dataquadinc.service.TimesheetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/users")
public class TimesheetController {

    @Autowired
    private TimesheetService timesheetService;

    @PostMapping("/checkin")
    public Timesheet logIn(@RequestBody Map<String, String> request) {
        String employeeId = request.get("employeeId");
        return timesheetService.logIn(employeeId);
    }

    @PutMapping("/checkout/{employeeId}")
    public ResponseEntity<?> logOut(@PathVariable String employeeId) {
        try {
            Timesheet updatedTimesheet = timesheetService.logOut(employeeId);
            return ResponseEntity.ok(updatedTimesheet);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping
    public List<Timesheet> getAllTimesheets() {
        return timesheetService.getAllTimesheets();
    }

    // New endpoint to get timesheets for a specific employee
    @GetMapping("/{employeeId}")
    public ResponseEntity<?> getEmployeeTimesheets(@PathVariable String employeeId) {
        List<Timesheet> employeeTimesheets = timesheetService.getTimesheetsByEmployeeId(employeeId);
        if (employeeTimesheets.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "No timesheets found for employee: " + employeeId
            ));
        }
        return ResponseEntity.ok(employeeTimesheets);
    }
}
