package com.dataquadinc.service;

import com.dataquadinc.model.UserDetails;
import com.dataquadinc.repository.UserDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserDao userDao;

    /**
     * Get all active IN entity employees excluding external candidates
     */
    public List<UserDetails> getActiveINEmpolyees() {
        // Using your existing repository method
        return userDao.findAllActiveInEmployeesExcludingExternal();
    }

    /**
     * Get employee by ID
     */
    public UserDetails getEmployeeById(String userId) {
        return userDao.findByUserId(userId);
    }

    /**
     * Get employees by department
     */
    public List<UserDetails> getEmployeesByDepartment(String department) {
        return userDao.findActiveInEmployeesByDepartment(department);
    }

    /**
     * Get employees by reporting manager
     */
    public List<UserDetails> getEmployeesByReportingManager(String managerId) {
        return userDao.findActiveInEmployeesByReportingManager(managerId);
    }

    /**
     * Get employees with pagination
     */
    public org.springframework.data.domain.Page<UserDetails> getEmployeesWithPagination(
            org.springframework.data.domain.Pageable pageable) {
        return userDao.findActiveInEmployeesWithPagination(pageable);
    }

    /**
     * Count active IN employees
     */
    public long countActiveINEmpolyees() {
        return userDao.countActiveInEmployeesExcludingExternal();
    }
}
