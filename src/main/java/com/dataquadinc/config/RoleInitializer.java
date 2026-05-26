package com.dataquadinc.config;

import com.dataquadinc.model.Roles;
import com.dataquadinc.model.UserType;
import com.dataquadinc.repository.RolesDao;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer implements CommandLineRunner {

    private final RolesDao rolesDao;

    public RoleInitializer(RolesDao rolesDao) {
        this.rolesDao = rolesDao;
    }

    @Override
    public void run(String... args) {
        for (UserType userType : UserType.values()) {
            rolesDao.findByName(userType).orElseGet(() -> {
                Roles role = new Roles();
                role.setName(userType);
                return rolesDao.save(role);
            });
        }
    }
}
