package com.dmsBackend.controller;

import com.dmsBackend.entity.Employee;
import com.dmsBackend.payloads.ApiResponse;
import com.dmsBackend.payloads.Helper;
import com.dmsBackend.repository.EmployeeRepository;
import com.dmsBackend.service.EmployeeService;
import com.dmsBackend.service.NotificationService;
import com.dmsBackend.service.RoleMasterService;
import com.dmsBackend.utils.AuditLogUtil;
import com.dmsBackend.utils.ResponseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.dmsBackend.response.MessageResponse;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/register")
//@CrossOrigin("https://happytyagi.github.io/DmsFrontend/")
public class RegisterController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private PasswordEncoder passwordEncode;

    @Autowired
    private RoleMasterService roleMasterService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogUtil auditLogUtil;

    @PostMapping("/save")
    @Transactional
    public ResponseEntity<?> saveEmployee(@RequestBody Employee employee) {
        log.info("Save employee request received. Email={}", employee.getEmail());

        try {
            String generatedPassword =
                    generatePassword(employee.getName(), employee.getMobile());

            employee.setPassword(generatedPassword);
            employee.setActive(false);

            User currentUser =
                    (User) SecurityContextHolder.getContext()
                            .getAuthentication()
                            .getPrincipal();

            Employee currentEmployee =
                    employeeService.findByEmail(currentUser.getUsername());

            if (currentEmployee == null) {
                log.warn("Current user not found while saving employee");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Current user not found");
            }

            Employee savedEmployee = employeeService.save(employee);

            sendPasswordEmail(employee.getEmail(), generatedPassword);

            log.info("Employee saved successfully. EmployeeId={}",
                    savedEmployee.getId());

            return ResponseEntity.ok(savedEmployee);

        } catch (Exception e) {
            log.error("Error saving employee: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving employee: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    @Transactional
    public ResponseEntity<ApiResponse> createEmployee(
            @Valid @RequestBody Employee employee,
            BindingResult result,
            HttpServletRequest request) {

        log.info("Create employee request received. Email={}, Mobile={}",
                employee.getEmail(), employee.getMobile());

        try {
            if (employeeRepository.existsByEmail(employee.getEmail())) {
                log.warn("Duplicate email detected: {}", employee.getEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new ApiResponse(
                                "An employee with the email address " + employee.getEmail()
                                        + " already exists. Please use a different email address.",
                                false)
                );
            }

            if (employeeRepository.existsByMobile(employee.getMobile())) {
                log.warn("Duplicate mobile detected: {}", employee.getMobile());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new ApiResponse(
                                "An employee with the mobile number " + employee.getMobile()
                                        + " already exists. Please use a different mobile number.",
                                false)
                );
            }

            if (result.hasErrors()) {
                String errors = result.getFieldErrors().stream()
                        .map(err -> err.getField() + ": " + err.getDefaultMessage())
                        .collect(Collectors.joining(", "));

                log.warn("Validation failed while creating employee: {}", errors);
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Validation errors: " + errors, false));
            }

            String generatedPassword =
                    generatePassword(employee.getName(), employee.getMobile());

            employee.setPassword(generatedPassword);

            Object principal =
                    SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            if (!(principal instanceof User)) {
                log.warn("Unauthorized access attempt while creating employee");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse("Unauthorized access. Please log in.", false));
            }

            User currentUser = (User) principal;
            Employee currentEmployee =
                    employeeService.findByEmail(currentUser.getUsername());

            if (currentEmployee == null) {
                log.warn("Current employee not authorized to create users");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(
                                "You do not have permission to create employees.", false));
            }

            employee.setCreatedBy(currentEmployee);
            employee.setCreatedOn(new Timestamp(System.currentTimeMillis()));

            Employee savedEmployee = employeeService.create(employee);

            notificationService.createNewEmployeeNotification(savedEmployee);
            sendPasswordEmail(employee.getEmail(), generatedPassword);

            Map<String, Object> detailsJson = Map.of(
                    "name", savedEmployee.getName(),
                    "email", savedEmployee.getEmail(),
                    "mobile", savedEmployee.getMobile(),
                    "branch", savedEmployee.getBranch() != null
                            ? savedEmployee.getBranch().getName() : null,
                    "department", savedEmployee.getDepartment() != null
                            ? savedEmployee.getDepartment().getName() : null
            );

            auditLogUtil.logAction(
                    currentEmployee,
                    "Users",
                    "Create",
                    "Success",
                    savedEmployee.getId(),
                    savedEmployee.getName(),
                    savedEmployee.getId(),
                    detailsJson,
                    request
            );

            log.info("Employee created successfully. EmployeeId={}",
                    savedEmployee.getId());

            return ResponseEntity.ok(
                    new ApiResponse("Employee created successfully.", true));

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating employee: {}",
                    e.getMessage(), e);

            String errorMessage =
                    "An entry with these details already exists. Please check and try again.";

            if (e.getMessage().contains("email")) {
                errorMessage =
                        "This email address is already registered. Please use a different email.";
            } else if (e.getMessage().contains("mobile")) {
                errorMessage =
                        "This mobile number is already in use. Please use a different mobile number.";
            }

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse(errorMessage, false));

        } catch (Exception e) {
            log.error("Unexpected error while creating employee: {}",
                    e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(
                            "We encountered an issue while processing your request. Please try again later.",
                            false));
        }
    }

    private String generatePassword(String username, String mobile) {
        String usernamePrefix =
                username.length() >= 4
                        ? username.substring(0, 4).toUpperCase()
                        : username.toUpperCase();

        String mobileSuffix =
                mobile.length() >= 4
                        ? mobile.substring(mobile.length() - 4)
                        : mobile;

        return usernamePrefix + mobileSuffix;
    }

    private void sendPasswordEmail(String email, String password) {
        log.info("Sending password email to {}", email);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your New Account Details");
        message.setText(
                "Dear Employee,\n\nYour account has been created. Here are your login details:\n\n" +
                        "Email: " + email + "\n" +
                        "Password: " + password + "\n\n" +
                        "Please login after your role is assigned." +
                        "Please change your password after logging in.\n\n" +
                        "Best regards,\nCompany Team"
        );

        mailSender.send(message);

        log.info("Password email sent successfully to {}", email);
    }
}
