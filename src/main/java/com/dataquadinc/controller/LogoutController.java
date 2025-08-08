package com.dataquadinc.controller;
import com.dataquadinc.dto.LogoutResponseDTO;
import com.dataquadinc.service.LogoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com","http://localhost:3000","http://192.168.0.135:8080","http://192.168.0.135",
        "http://154.210.288.26",
        "http://192.168.0.203:3000",
        "http://192.168.0.167:3000"})

@RestController
@RequestMapping("/users")
public class  LogoutController {

    private final LogoutService logoutService;

    @Autowired
    public LogoutController(LogoutService logoutService) {
        this.logoutService = logoutService;
    }

    @PutMapping("/logout/{userId}")
    public LogoutResponseDTO logout(@PathVariable String userId) {

        return logoutService.logout(userId);
    }
}


