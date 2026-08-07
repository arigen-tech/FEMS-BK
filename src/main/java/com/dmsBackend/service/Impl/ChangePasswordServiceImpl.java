package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.Employee;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.repository.EmployeeRepository;
import com.dmsBackend.response.ChangePasswordDTO;
import com.dmsBackend.service.ChangePasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

@Service
public class ChangePasswordServiceImpl implements ChangePasswordService {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordServiceImpl.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        Employee employee = employeeRepository.findByEmail(changePasswordDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with email: " + changePasswordDTO.getEmail()));

        // Check if the current password matches
        if (!passwordEncoder.matches(changePasswordDTO.getCurrentPassword(), employee.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Encode the new password
        String encodedNewPassword = passwordEncoder.encode(changePasswordDTO.getNewPassword());

        // Update the employee's password
        employee.setPassword(encodedNewPassword);
        employeeRepository.save(employee);
    }
}
