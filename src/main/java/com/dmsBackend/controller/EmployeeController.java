package com.dmsBackend.controller;

import com.dmsBackend.entity.*;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.ApiResponse;
import com.dmsBackend.repository.EmployeeRepository;
import com.dmsBackend.response.*;
import com.dmsBackend.security.EmailService;
import com.dmsBackend.service.*;
import com.dmsBackend.utils.AuditLogUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/employee")
//@CrossOrigin("https://happytyagi.github.io/DmsFrontend/")
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;
    private final RoleMasterService roleService;
    private final BranchMasterService branchService;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private  EmployeeRoleService employeeRoleService;
    //private final RoleMasterService roleMasterService;
    @Autowired
    private RoleMasterService roleMasterService;

    @Autowired
    private DepartmentMasterService departmentMasterService;

    @Autowired
    private EmailService emailService;

    @Lazy
    @Autowired
    private NotificationService notificationService;

    public EmployeeController(
            EmployeeService employeeService,
            RoleMasterService roleService,
            BranchMasterService branchService,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender) {
        this.employeeService = employeeService;
        this.roleService = roleService;
        this.branchService = branchService;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.roleMasterService = roleMasterService;
    }

    @Autowired
    EmployeeRepository employeeRepository;
    @Autowired
    AuditLogUtil auditLogUtil;


    @PutMapping("/update/{id}")
    @Transactional
    public ResponseEntity<?> updateEmployee(
            @PathVariable Integer id,
            @RequestBody Employee employeeDetails,
            HttpServletRequest request) {

        log.info("API CALL → Update Employee | employeeId={}", id);

        try {
            // ✅ Get the current authenticated user
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Employee currentEmployee = employeeService.findByEmail(currentUser.getUsername());

            if (currentEmployee == null) {
                log.error("Current user not found for update | username={}", currentUser.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Current user not found");
            }

            log.debug("Current authenticated user | userId={} username={}", currentEmployee.getId(), currentUser.getUsername());

            // ✅ Fetch existing employee (before update) to capture old data
            Optional<Employee> existingOpt = employeeRepository.findById(id);
            if (existingOpt.isEmpty()) {
                log.error("Employee not found for update | employeeId={}", id);

                // 🔴 Audit failure log
                auditLogUtil.logAction(
                        currentEmployee,
                        "EmployeePage",
                        "Update",
                        "Failure",
                        id,
                        null,
                        id,
                        Map.of("error", "Employee not found"),
                        request
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found");
            }

            Employee existingEmployee = existingOpt.get();
            log.debug("Found existing employee | employeeId={} name={} email={}",
                    id, existingEmployee.getName(), existingEmployee.getEmail());

            // ✅ Capture previous data
            Map<String, Object> previousData = Map.of(
                    "name", existingEmployee.getName(),
                    "email", existingEmployee.getEmail(),
                    "mobile", existingEmployee.getMobile(),
                    "branch", existingEmployee.getBranch() != null ? existingEmployee.getBranch().getName() : null,
                    "department", existingEmployee.getDepartment() != null ? existingEmployee.getDepartment().getName() : null
            );

            log.debug("Previous employee data | employeeId={} data={}", id, previousData);

            // ✅ Call service to update the employee details
            com.dmsBackend.response.ApiResponse<Employee> apiResponse = employeeService.updateEmployeeDetails(
                    id,
                    employeeDetails.getName(),
                    employeeDetails.getEmail(),
                    employeeDetails.getMobile(),
                    employeeDetails.getBranch() != null ? employeeDetails.getBranch().getId() : null,
                    employeeDetails.getDepartment() != null ? employeeDetails.getDepartment().getId() : null
            );

            // If update fails
            if (apiResponse.getStatus() != HttpStatus.OK.value()) {
                log.error("FAILED → Update Employee | employeeId={} reason={}",
                        id, apiResponse.getMessage());

                // 🔴 Audit failure log
                auditLogUtil.logAction(
                        currentEmployee,
                        "EmployeePage",
                        "Update",
                        "Failure",
                        id,
                        existingEmployee.getName(),
                        id,
                        previousData,
                        request
                );
                return ResponseEntity.status(apiResponse.getStatus())
                        .body(apiResponse.getMessage());
            }

            // ✅ Audit success log
            auditLogUtil.logAction(
                    currentEmployee,
                    "EmployeePage",
                    "Update",
                    "Success",
                    id,
                    apiResponse.getResponse().getName(),
                    id,
                    previousData,
                    request
            );

            // ✅ Send email (non-transactional)
            try {
                log.debug("Sending update notification email | employeeId={} email={}",
                        id, apiResponse.getResponse().getEmail());
                emailService.sendUpdateNotification(apiResponse.getResponse());
                log.info("Update notification email sent | employeeId={}", id);
            } catch (Exception emailException) {
                log.error("Failed to send update notification email | employeeId={} reason={}",
                        id, emailException.getMessage(), emailException);
                // log but don't rollback
            }

            log.info("SUCCESS → Employee Updated | employeeId={} updatedBy={}",
                    id, currentEmployee.getId());

            return ResponseEntity.ok(apiResponse.getResponse());

        } catch (Exception e) {
            log.error("ERROR → Update Employee | employeeId={} reason={}",
                    id, e.getMessage(), e);

            // 🔴 Audit error log
            auditLogUtil.logAction(
                    null,
                    "Users",
                    "Update",
                    "Error",
                    id,
                    null,
                    id,
                    Map.of("exception", e.getMessage()),
                    request
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating employee: " + e.getMessage());
        }
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteByIdEmployee(@PathVariable Integer id) {
        log.info("API CALL → Delete Employee | employeeId={}", id);

        try {
            employeeService.deleteByIdEmployee(id);
            log.info("SUCCESS → Employee Deleted | employeeId={}", id);
            return ResponseEntity.ok(new ApiResponse("Employee deleted successfully.", true));
        } catch (ResourceNotFoundException e) {
            log.error("FAILED → Delete Employee | employeeId={} reason={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), false));
        } catch (Exception e) {
            log.error("ERROR → Delete Employee | employeeId={} reason={}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(new ApiResponse("Error deleting employee: " + e.getMessage(), false));
        }
    }

    @GetMapping("/findAll")
    public ResponseEntity<List<EmployeeDTO>> findAllEmployee() {
        log.info("API CALL → Find All Employees");

        List<Employee> allEmployees = employeeService.findAllEmployee();
        List<EmployeeDTO> employeeDTOs = allEmployees.stream()
                .map(employeeService::mapToDTO) // Calls updated mapToDTO
                .toList();

        log.info("SUCCESS → Retrieved {} employees", employeeDTOs.size());
        return ResponseEntity.ok(employeeDTOs);
    }


    @GetMapping("/findById/{id}")
    public ResponseEntity<Employee> findByIdEmployee(@PathVariable Integer id) {
        log.info("API CALL → Find Employee By ID | employeeId={}", id);

        Employee employee = employeeService.findById(id);

        if (employee != null) {
            log.debug("Found employee | employeeId={} name={} email={}",
                    id, employee.getName(), employee.getEmail());
        }

        log.info("SUCCESS → Retrieved employee | employeeId={}", id);
        return ResponseEntity.ok(employee);
    }

    @PutMapping("/updateStatus/{id}")
    public ResponseEntity<ApiResponse> updateEmployeeStatus(@PathVariable Integer id, @RequestBody Boolean isActive, HttpServletRequest request) {
        log.info("API CALL → Update Employee Status | employeeId={} isActive={}", id, isActive);

        try {
            employeeService.updateEmployeeStatus(id, isActive, request);
            log.info("SUCCESS → Employee Status Updated | employeeId={} isActive={}", id, isActive);
            return ResponseEntity.ok(new ApiResponse("Employee status updated successfully.", true));
        } catch (Exception e) {
            log.error("FAILED → Update Employee Status | employeeId={} reason={}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error updating employee status: " + e.getMessage(), false));
        }
    }

    //update role
//    @PutMapping("/employee/{identifier}/role")
//    public ResponseEntity<?> updateEmployeeRole(@PathVariable String identifier, @RequestBody Map<String, String> requestBody) {
//        String roleName = requestBody.get("roleName");
//
//        if (roleName == null || roleName.isEmpty()) {
//            return ResponseEntity.badRequest().body("Role name must not be null or empty.");
//        }
//
//        try {
//            // Find the role by name and get its ID
//            Integer roleId = roleService.findRoleByName(roleName)
//                    .orElseThrow(() -> new ResourceNotFoundException("Invalid role name: " + roleName))
//                    .getId();
//
//            Employee updatedEmployee;
//
//            // Check if the identifier is a valid integer (ID) or an email
//            if (identifier.matches("\\d+")) {
//                // Identifier is a numeric ID
//                Integer employeeId = Integer.parseInt(identifier);
//                employeeService.updateEmployeeRoleById(employeeId, roleId);
//                updatedEmployee = employeeService.findById(employeeId); // Get updated employee details
//            } else {
//                // Identifier is an email
//                employeeService.updateEmployeeRoleByEmail(identifier, roleId);
//                updatedEmployee = employeeService.findByEmail(identifier); // Get updated employee details
//            }
//
//            notifyUserRole(updatedEmployee.getEmail(), roleName);
//            return ResponseEntity.ok("Role updated successfully.");
//        } catch (ResourceNotFoundException e) {
//            return ResponseEntity.badRequest().body("Error updating role: " + e.getMessage());
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("Error updating role: " + e.getMessage());
//        }
//    }



    @PutMapping("/{identifier}/role")
    public ResponseEntity<?> updateEmployeeRole(
            @PathVariable String identifier,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request
    ) {
        log.info("API CALL → Update Employee Role | identifier={}", identifier);

        String roleName = requestBody.get("roleName");

        // Validate the role name
        if (roleName == null || roleName.isEmpty()) {
            log.error("Role name is null or empty | identifier={}", identifier);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Role name must not be null or empty"));
        }

        log.debug("Updating employee role | identifier={} roleName={}", identifier, roleName);

        try {
            // Update employee role
            Integer employeeId = Integer.parseInt(identifier);
            Employee updatedEmployee = employeeService.updateEmployeeRoleByName(employeeId, roleName, request);

            log.debug("Employee role updated successfully | employeeId={} newRole={}",
                    employeeId, roleName);

            // Notify the user of the role change
            try {
                notifyUserRole(updatedEmployee.getEmail(), roleName);
                log.debug("Role change notification sent | employeeId={} email={}",
                        employeeId, updatedEmployee.getEmail());
            } catch (Exception emailEx) {
                log.warn("Failed to send role change notification | employeeId={} reason={}",
                        employeeId, emailEx.getMessage());
                // Continue even if email fails
            }

            log.info("SUCCESS → Employee Role Updated | employeeId={} newRole={}",
                    employeeId, roleName);

            return ResponseEntity.ok(
                    Map.of("message", "Role updated successfully for employee with identifier: " + identifier)
            );
        } catch (NumberFormatException e) {
            log.error("Invalid employee identifier format | identifier={}", identifier);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid employee identifier format"));
        } catch (ResourceNotFoundException e) {
            log.error("Employee or role not found | identifier={} reason={}", identifier, e.getMessage());
            return ResponseEntity.status(404)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("ERROR → Update Employee Role | identifier={} reason={}",
                    identifier, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "An unexpected error occurred while updating the role: " + e.getMessage()));
        }
    }

    //find by role name
    @GetMapping("/role/{roleName}")
    public ResponseEntity<List<Employee>> getEmployeesByRole(@PathVariable String roleName) {
        log.info("API CALL → Get Employees By Role | roleName={}", roleName);

        List<Employee> employees = employeeService.findEmployeesByRole(roleName);

        log.info("SUCCESS → Retrieved {} employees with role {}", employees.size(), roleName);
        return new ResponseEntity<>(employees, HttpStatus.OK);
    }

    @GetMapping("/status-count-by-year")
    public ResponseEntity<List<StatusCountByYearDto>> getStatusCountsByYear() {
        log.info("API CALL → Get Status Counts By Year");

        List<StatusCountByYearDto> result = employeeService.getStatusCountsPerYear();

        log.info("SUCCESS → Retrieved status counts for {} years", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/role-null/{id}")
    public ResponseEntity<List<Employee>> getEmployeesByRoleIsNullById(@PathVariable Integer id) {
        log.info("API CALL → Get Employees By Role Is Null By ID | id={}", id);

        List<Employee> employees = employeeService.getEmployeesByRoleIsNullById(id);

        if (employees.isEmpty()) {
            log.info("No employees found with null role for ID {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(employees);
        }

        log.info("SUCCESS → Retrieved {} employees with null role for ID {}", employees.size(), id);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<EmployeeDTO>> findEmployeesByBranch(@PathVariable Integer branchId) {
        log.info("API CALL → Find Employees By Branch | branchId={}", branchId);

        // Fetch the BranchMaster entity using the provided branchId
        BranchMaster branch = branchService.findByIdBran(branchId);

        if (branch == null) {
            log.error("Branch not found | branchId={}", branchId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        log.debug("Found branch | branchId={} branchName={}", branchId, branch.getName());

        // Fetch employees by branch and map to DTOs
        List<EmployeeDTO> employeeDTOs = employeeService.findEmployeesByBranch(branch);

        log.info("SUCCESS → Retrieved {} employees for branch {}", employeeDTOs.size(), branchId);
        return ResponseEntity.ok(employeeDTOs);
    }



    @GetMapping("/role-is-null")
    public ResponseEntity<List<Employee>> getEmployeesByRoleIsNull() {
        log.info("API CALL → Get Employees By Role Is Null");

        List<Employee> employees = employeeService.getEmployeesByRoleIsNull();

        log.info("SUCCESS → Retrieved {} employees with null role", employees.size());
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/role-not-null")
    public ResponseEntity<List<Employee>> getAllWithoutNullRole() {
        log.info("API CALL → Get All Employees Without Null Role");

        List<Employee> employees = employeeService.getAllWithoutNullRole();

        log.info("SUCCESS → Retrieved {} employees with non-null roles", employees.size());
        return ResponseEntity.ok(employees);
    }

//    @GetMapping("/role-not-null/dep/{depId}")
//    public ResponseEntity<List<Employee>> findAllByRoleIsNotNullAndDepartment(@PathVariable Long depId) {
//        try {
//            // Fetch the DepartmentMaster by ID using the department service
//            DepartmentMaster department = departmentService.findById(depId);
//
//            // Fetch employees where the role is not null and they belong to the specified department
//            List<Employee> employees = employeeService.findAllByRoleIsNotNullAndDepartment(department);
//
//            // Return the list of employees
//            return ResponseEntity.ok(employees);
//        } catch (Exception e) {
//            // Handle exceptions and return an appropriate error response
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }

    @GetMapping("/role-not-null/branch/{branchId}")
    public ResponseEntity<List<Employee>> findAllByRoleIsNotNullAndBranch(@PathVariable BranchMaster branchId) {
        log.info("API CALL → Find All By Role Is Not Null And Branch | branchId={}", branchId.getId());

        List<Employee> employees = employeeService.findAllByRoleIsNotNullAndBranch(branchId);

        log.info("SUCCESS → Retrieved {} employees with non-null roles for branch {}",
                employees.size(), branchId.getId());
        return ResponseEntity.ok(employees);
    }

//    @GetMapping("/pending-by-branch")
//    public ResponseEntity<?> getPendingEmployeesByCurrentUserBranch() {
//        try {
//            // Get the current authenticated user
//            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//            Employee currentEmployee = employeeService.findByEmail(currentUser.getUsername());
//
//            if (currentEmployee == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Current user not found");
//            }
//
//            // Check if the current user has the ADMIN role
//            boolean isAdmin = currentUser.getAuthorities().stream()
//                    .anyMatch(authority -> authority.getAuthority().equals("ADMIN"));
//
//            List<Employee> pendingEmployees;
//            if (isAdmin) {
//                // Admin can view pending employees from all branches
//                pendingEmployees = employeeService.findEmployeesWithNullRole();
//            } else if (currentEmployee.getBranch() != null) {
//                // Non-admin users can only view pending employees from their own branch
//                pendingEmployees = employeeService.findEmployeesWithNullRoleByBranch(currentEmployee.getBranch());
//            } else {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Current user's branch not found");
//            }
//
//            return ResponseEntity.ok(pendingEmployees);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching pending employees: " + e.getMessage());
//        }
//    }

    @GetMapping("/pending-by-branch")
    public ResponseEntity<?> getPendingEmployeesByCurrentUserBranch() {
        log.info("API CALL → Get Pending Employees By Current User Branch");

        try {
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Employee currentEmployee = employeeService.findByEmail(currentUser.getUsername());

            if (currentEmployee == null) {
                log.error("Current user not found | username={}", currentUser.getUsername());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Current user not found");
            }

            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("SYSTEM ADMIN"));

            log.debug("User authentication check | userId={} isAdmin={}",
                    currentEmployee.getId(), isAdmin);

            List<Employee> pendingEmployees;
            if (isAdmin) {
                log.debug("Admin user - fetching all pending employees");
                pendingEmployees = employeeService.findEmployeesWithNullRole();
            } else if (currentEmployee.getBranch() != null) {
                log.debug("Non-admin user - fetching pending employees for branch | branchId={}",
                        currentEmployee.getBranch().getId());
                pendingEmployees = employeeService.findEmployeesWithNullRoleByBranch(currentEmployee.getBranch());
            } else {
                log.error("Current user's branch not found | userId={}", currentEmployee.getId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Current user's branch not found");
            }

            // Map entities to DTOs
            List<EmployeeDTO> employeeDTOs = pendingEmployees.stream()
                    .map(employeeService::mapToDTO)
                    .toList();

            log.info("SUCCESS → Retrieved {} pending employees", employeeDTOs.size());
            return ResponseEntity.ok(employeeDTOs);
        } catch (Exception e) {
            log.error("ERROR → Get Pending Employees By Current User Branch | reason={}",
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching pending employees: " + e.getMessage());
        }
    }



    @GetMapping("/pending-by-department")
    public ResponseEntity<?> getPendingEmployeesByCurrentUserDepartment() {
        log.info("API CALL → Get Pending Employees By Current User Department");

        try {
            // Get the current authenticated user
            User currentUser = (User) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            Employee currentEmployee = employeeService.findByEmail(currentUser.getUsername());

            if (currentEmployee == null) {
                log.error("Current user not found | username={}", currentUser.getUsername());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Current user not found");
            }

            // Check if the current user has the ADMIN role
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ADMIN"));

            log.debug("User authentication check | userId={} isAdmin={}",
                    currentEmployee.getId(), isAdmin);

            List<Employee> pendingEmployees;
            if (isAdmin) {
                log.debug("Admin user - fetching all pending employees");
                // Admin can view pending employees from all departments
                pendingEmployees = employeeService.findEmployeesWithNullRole();
            } else if (currentEmployee.getDepartment() != null) {
                log.debug("Non-admin user - fetching pending employees for department | departmentId={}",
                        currentEmployee.getDepartment().getId());
                // Non-admin users can only view pending employees from their own department
                pendingEmployees = employeeService.findEmployeesWithNullRoleByDepartment(
                        currentEmployee.getDepartment()
                );
            } else {
                log.error("Current user's department not found | userId={}", currentEmployee.getId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Current user's department not found");
            }

            List<EmployeeDTO> employeeDTOs = pendingEmployees.stream()
                    .map(employeeService::mapToDTO)
                    .toList();

            log.info("SUCCESS → Retrieved {} pending employees", employeeDTOs.size());
            return ResponseEntity.ok(employeeDTOs);
        } catch (Exception e) {
            log.error("ERROR → Get Pending Employees By Current User Department | reason={}",
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching pending employees: " + e.getMessage());
        }
    }



    //===========================role msg=============
    private void notifyUserRole(String email, String roleName) {
        log.debug("Sending role assignment notification | email={} role={}", email, roleName);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Role Assignment Notification");
        message.setText("Dear Employee,\n\n" +
                "We are pleased to inform you that your role has been successfully updated.\n\n" +
                "Assigned Role: " + roleName + "\n\n" +
                "Please log in to your account to review your updated role. For security purposes, we recommend changing your password after your next login.\n\n" +
                "Best regards,\n" +
                "The Company Team");

        // Try to send the email and handle any failures
        try {
            mailSender.send(message);
            log.info("Role assignment notification sent | email={}", email);
        } catch (Exception e) {
            log.error("Failed to send role assignment notification | email={} reason={}",
                    email, e.getMessage(), e);
        }
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<com.dmsBackend.response.ApiResponse<List<EmployeeDTO>>> getEmployeesByDepartment(@PathVariable("departmentId") DepartmentMaster department) {
        log.info("API CALL → Get Employees By Department | departmentId={}", department.getId());

        com.dmsBackend.response.ApiResponse<List<EmployeeDTO>> employees = employeeService.findEmployeesByDepartment(department);

        if (employees.getResponse() == null || employees.getResponse().isEmpty()) {
            log.info("No employees found for department | departmentId={}", department.getId());
            return ResponseEntity.noContent().build();
        }

        log.info("SUCCESS → Retrieved {} employees for department {}",
                employees.getResponse().size(), department.getId());
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/employeeCreateby/{empId}")
    public ResponseEntity<com.dmsBackend.response.ApiResponse<List<Employee>>> getEmployeeByCreatedBy(
            @PathVariable("empId") Integer empId) {
        log.info("API CALL → Get Employee By Created By | empId={}", empId);

        Employee employee = employeeService.findById(empId); // Ensure this method exists in your service
        if (employee == null) {
            log.error("Employee not found | empId={}", empId);
            throw new IllegalArgumentException("Employee with ID " + empId + " not found.");
        }

        com.dmsBackend.response.ApiResponse<List<Employee>> response = employeeService.findEmployeeByCreatedByEmp(employee);

        log.info("SUCCESS → Retrieved {} employees created by empId={}",
                response.getResponse().size(), empId);
        return ResponseEntity.ok(response);
    }



    @PostMapping("report/filter")
    public ResponseEntity<com.dmsBackend.response.ApiResponse<FileResponse>> getFilteredEmployees(
            @RequestBody EmployeeFilterRequest employeeFilterRequest) {
        log.info("API CALL → Get Filtered Employees Report");

        com.dmsBackend.response.ApiResponse<FileResponse> apiResponse = employeeService.getFilteredEmployeesApiResponse(employeeFilterRequest);

        log.info("SUCCESS → Generated filtered employees report | status={}", apiResponse.getStatus());
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }







//    @PostMapping("report/filter")
//    public ResponseEntity<?> getFilteredEmployees(@RequestBody EmployeeFilterRequest filterRequest) {
//        // Extract filter parameters from the request
//
//        Integer departmentMasterId = filterRequest.getDepartmentMasterId();
//        Boolean status = filterRequest.getStatus();
//        Timestamp startDate = filterRequest.getStartDate();
//        Timestamp endDate = filterRequest.getEndDate();
//        String docType = filterRequest.getDocType();
//
//        // Fetch filtered employees based on the filter parameters
//        List<EmployeeResponse> employees = employeeService.getFilteredEmployees(
//                departmentMasterBranchId, departmentMasterId, status, startDate, endDate);
//
//        // Set dynamic values for branch and department names (to be replaced with actual values)
//        String branchName = employees.isEmpty() ? "No Branch" : employees.get(0).getBranchName();
//        String departmentName = employees.isEmpty() ? "No department" : employees.get(0).getDepartmentName();
//
//        // Handle status as a string (active or inactive)
//        String statusString = (status != null) ? (status ? "active" : "inactive") : "";
//
//        // Format start and end dates
//        String formattedStartDate = (startDate != null) ? new SimpleDateFormat("dd/MM/yyyy").format(startDate) : "";
//        String formattedEndDate = (endDate != null) ? new SimpleDateFormat("dd/MM/yyyy").format(endDate) : "";
//        // Check if employees are found, if not, return a proper response
//        if (employees.isEmpty()) {
//            return ResponseEntity.noContent().build();
//        }
//
//        // Handle document type (PDF or EXCEL)
//        if ("PDF".equalsIgnoreCase(docType)) {
//            try {
//                // Generate PDF if requested
//                byte[] pdfData = pdfGenerator.generatePdf(employees, branchName, departmentName,
//                        statusString, formattedStartDate, formattedEndDate);
//                String dynamicFileName = pdfGenerator.getDynamicFileName(branchName, departmentName);
//                return ResponseEntity.ok()
//                        .header("Content-Type", "application/pdf")
//                        .header("Content-Disposition", "attachment; filename=" + dynamicFileName)
//                        .body(pdfData);
//            } catch (Exception e) {
//                // Handle exception (e.g., log it and return a server error response)
//                e.printStackTrace();
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                        .body("An error occurred while generating the PDF: " + e.getMessage());
//            }
//        } else if ("EXCEL".equalsIgnoreCase(docType)) {
//            // Generate Excel if requested
//            byte[] excelData = excelGenerator.generateExcel(employees);
//            return ResponseEntity.ok()
//                    .header("Content-Type", "application/vnd.ms-excel")
//                    .header("Content-Disposition", "attachment; filename=employees.xlsx")
//                    .body(excelData);
//        } else {
//            // If neither PDF nor Excel is requested, return the employee data as JSON
//            return ResponseEntity.ok(employees);
//        }
//    }


    // Endpoint to upload profile image
    @PostMapping("/upload/{employeeId}")
    public ResponseEntity<?> uploadProfileImage(
            @PathVariable Integer employeeId,
            @RequestParam("file") MultipartFile file) {
        log.info("API CALL → Upload Profile Image | employeeId={} fileName={} fileSize={}",
                employeeId, file.getOriginalFilename(), file.getSize());

        try {
            // Find employee and save profile image
            Employee employee = employeeService.findById(employeeId);
            employeeService.saveProfileImage(employee, file);

            log.info("SUCCESS → Profile Image Uploaded | employeeId={} fileName={}",
                    employeeId, file.getOriginalFilename());
            return ResponseEntity.ok("Image uploaded and saved successfully.");
        } catch (RuntimeException ex) {
            log.error("FAILED → Upload Profile Image | employeeId={} reason={}",
                    employeeId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @GetMapping("/getImageSrc/{employeeId}")
    public ResponseEntity<?> getProfileImageSrc(@PathVariable Integer employeeId) {
        log.info("API CALL → Get Profile Image Src | employeeId={}", employeeId);

        Employee employee = employeeService.findById(employeeId);

        ProfileImage profileImage = employeeService.findByEmployee(employee)
                .orElseThrow(() -> {
                    log.error("Profile image not found | employeeId={}", employeeId);
                    return new ResourceNotFoundException("Profile image not found", "Employee ID", employeeId);
                });

        String imagePath = profileImage.getDpImageSrc(); // e.g., D:/Dheeraj_Codes/Backend/Java/Projects/dms/Doc_Dp_Image/photo.JPG

        File imageFile = new File(imagePath);

        if (!imageFile.exists()) {
            log.error("Image file not found | employeeId={} imagePath={}", employeeId, imagePath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image not found");
        }

        try {
            // Read the image as a byte array
            Path path = Paths.get(imagePath);
            byte[] imageBytes = Files.readAllBytes(path);

            log.info("SUCCESS → Retrieved profile image | employeeId={} imagePath={}", employeeId, imagePath);

            // Return the image as a byte array with the correct MIME type
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // Assuming it's a JPEG; adjust based on actual image type
                    .body(imageBytes);
        } catch (IOException e) {
            log.error("ERROR → Reading profile image | employeeId={} imagePath={} reason={}",
                    employeeId, imagePath, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading image");
        }
    }

    @PutMapping("/update/profile")
    public ResponseEntity<Employee> updateProfile(@RequestBody Employee employee) {
        log.info("API CALL → Update Profile");

        try {
            // Get the logged-in employee's ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Object principal = authentication.getPrincipal();
            Integer loggedInEmployeeId = null;

            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                String email = userDetails.getUsername();
                log.debug("Updating profile for authenticated user | email={}", email);
                Employee loggedInEmployee = employeeService.findByEmail(email);
                loggedInEmployeeId = loggedInEmployee.getId();
            }

            Employee updatedEmployee = employeeService.updateProfile(employee, loggedInEmployeeId);

            log.info("SUCCESS → Profile Updated | employeeId={}", loggedInEmployeeId);
            return new ResponseEntity<>(updatedEmployee, HttpStatus.OK);
        } catch (Exception e) {
            log.error("ERROR → Update Profile | reason={}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{employeeId}/role/switch")
    public ResponseEntity<com.dmsBackend.response.ApiResponse<Employee>> switchEmployeeRole(
            @PathVariable String employeeId,
            @RequestBody RoleSwitchRequest roleSwitchRequest) {

        log.info("API CALL → Switch Employee Role | employeeId={} targetRole={}",
                employeeId, roleSwitchRequest.getTargetRoleName());

        try {
            // Retrieve current authenticated user
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Employee currentEmployee = employeeService.findByEmail(currentUser.getUsername());

            log.debug("Current authenticated user | userId={} username={}",
                    currentEmployee.getId(), currentUser.getUsername());

            // Call service method to switch role
            com.dmsBackend.response.ApiResponse<Employee> updatedEmployee = employeeService.switchEmployeeRole(
                    employeeId,
                    roleSwitchRequest.getTargetRoleName(),
                    currentEmployee
            );

            log.info("SUCCESS → Employee Role Switched | employeeId={} newRole={}",
                    employeeId, roleSwitchRequest.getTargetRoleName());

            // Return success response
            return new ResponseEntity<>(updatedEmployee, HttpStatus.OK);

        } catch (IllegalStateException e) {
            // Handle the case when the target role is inactive
            log.error("FAILED → Switch Employee Role | employeeId={} reason={}",
                    employeeId, e.getMessage());

            com.dmsBackend.response.ApiResponse<Employee> errorResponse = new com.dmsBackend.response.ApiResponse<>();
            errorResponse.setMessage(e.getMessage());  // Set the message
            errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());  // Set the status code

            // Return bad request response with the error message
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);

        } catch (ResourceNotFoundException e) {
            // Handle the case when the employee or role is not found
            log.error("FAILED → Switch Employee Role | employeeId={} reason={}",
                    employeeId, e.getMessage());

            com.dmsBackend.response.ApiResponse<Employee> errorResponse = new com.dmsBackend.response.ApiResponse<>();
            errorResponse.setMessage(e.getMessage());  // Set the message
            errorResponse.setStatus(HttpStatus.NOT_FOUND.value());  // Set the status code

            // Return not found response with the error message
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            // Handle generic errors
            log.error("ERROR → Switch Employee Role | employeeId={} reason={}",
                    employeeId, e.getMessage(), e);

            com.dmsBackend.response.ApiResponse<Employee> errorResponse = new com.dmsBackend.response.ApiResponse<>();
            errorResponse.setMessage("An unexpected error occurred.");
            errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

            // Return internal server error response
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/current/branch-department")
    public com.dmsBackend.response.ApiResponse<List<EmployeeDTO>> getEmployeesOfCurrentUsersBranchAndDepartment() {
        log.info("API CALL → Get Employees Of Current User's Branch And Department");

        com.dmsBackend.response.ApiResponse<List<EmployeeDTO>> response =
                employeeService.getEmployeesOfCurrentUserBranchAndDepartment();

        log.info("SUCCESS → Retrieved {} employees for current user's branch and department",
                response.getResponse().size());
        return response;
    }


}