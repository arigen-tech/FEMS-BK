package com.dmsBackend.security;

import com.dmsBackend.entity.Employee;
import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailService implements UserDetailsService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Fetch employee by email, which returns Optional<Employee>
        Optional<Employee> optionalEmployee = employeeRepository.findByEmail(email);

        // Check if employee is present, otherwise throw an exception
        Employee employee = optionalEmployee.orElseThrow(() ->
                new UsernameNotFoundException("User not found with email: " + email)
        );

        // Fetch the employee's role (assuming one role per employee)
        RoleMaster role = employee.getRole();

        if (role == null) {
            throw new ResourceNotFoundException("Employee role not found", "Email", email);
        }

        // Return the UserDetails object with the role as an authority
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role.getRole());

        return new org.springframework.security.core.userdetails.User(
                employee.getEmail(),
                employee.getPassword(),
                Collections.singletonList(authority) // Assign the role as an authority
        );
    }





}
