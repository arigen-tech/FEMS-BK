package com.dmsBackend.utils;

import com.dmsBackend.entity.Employee;
import com.dmsBackend.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    @Autowired
    @Lazy
    EmployeeService employeeService;
    public Employee getCurrentEmployeeOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : null;

        if (username == null) {
            throw new RuntimeException("Current user not found");
        }

        Employee currentEmployee = employeeService.findByEmail(username);
        if (currentEmployee == null) {
            throw new RuntimeException("Current user not found");
        }

        return currentEmployee;
    }

}
