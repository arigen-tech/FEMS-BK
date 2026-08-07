package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.*;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.Helper;
import com.dmsBackend.repository.*;
import com.dmsBackend.response.*;
import com.dmsBackend.security.EmailService;
import com.dmsBackend.service.EmployeeService;
import com.dmsBackend.service.NotificationService;
import com.dmsBackend.utils.AuditLogUtil;
import com.dmsBackend.utils.CurrentUser;
import com.dmsBackend.utils.ResponseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    EmpPdfGenerator pdfGenerator;
    @Autowired
    private ProfileImageRepository profileImageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleMasterRepository roleMasterRepository;

    @Autowired
    private DepartmentMasterRepository departmentMasterRepository;

    @Autowired
    private BranchMasterRepository branchMasterRepository;

    @Autowired
    private EmployeeRoleRepository employeeRoleRepository;

    @Autowired
    private EmailService emailService;

    @Lazy
    @Autowired
    private NotificationService notificationService;

    @Value("${document.storage.path}")
    private String documentStoragePath;

    private final JavaMailSender mailSender;

    @Autowired
    AuditLogUtil auditLogUtil;

    @Autowired

    CurrentUser currentUser;

    @Autowired
    LanguageMasterRepository languageMasterRepository;

    @Autowired
    private EmployeeIdGenerator employeeIdGenerator;

    public EmployeeServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }


    @Override
    @Transactional
    public Employee create(Employee employee) {
        log.info("API CALL → Create Employee | email={}", employee.getEmail());

        // Set timestamps
        employee.setCreatedOn(Helper.getCurrentTimeStamp());
        employee.setUpdatedOn(Helper.getCurrentTimeStamp());

        // Find and set branch
        if (employee.getBranch() != null && employee.getBranch().getId() != null) {
            BranchMaster branchMaster = branchMasterRepository.findById(employee.getBranch().getId())
                    .orElseThrow(() -> new RuntimeException("Branch Not Found"));
            employee.setBranch(branchMaster);
        }

        // Generate unique Employee ID
        employee.setEmployeeId(employeeIdGenerator.generateEmployeeId());

        // Set active status and encode password
        employee.setActive(false);
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));

        // Save the employee
        Employee savedEmployee = employeeRepository.save(employee);

        log.info("SUCCESS → Employee Created | id={} name={} employeeId={}",
                savedEmployee.getId(), savedEmployee.getName(), savedEmployee.getEmployeeId());

        return savedEmployee;
    }

    @Override
    @Transactional
    public Employee save(Employee employee) {
        log.info("API CALL → Save/Update Employee | id={} name={}", employee.getId(), employee.getName());

        // Fetch the existing employee from the database
        Employee existingEmployee = employeeRepository.findById(employee.getId())
                .orElseThrow(() -> new RuntimeException("Employee Not Found"));

        // Retain the existing password if the new password is null or empty
        if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
            employee.setPassword(existingEmployee.getPassword());
        } else {
            // Encode the new password before saving
            employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        }

        // Set updated timestamps
        employee.setCreatedOn(existingEmployee.getCreatedOn()); // Retain createdOn timestamp
        employee.setUpdatedOn(Helper.getCurrentTimeStamp());

        // Find and set branch
        if (employee.getBranch() != null && employee.getBranch().getId() != null) {
            BranchMaster branchMaster = branchMasterRepository.findById(employee.getBranch().getId())
                    .orElseThrow(() -> new RuntimeException("Branch Not Found"));
            employee.setBranch(branchMaster);
        } else {
            employee.setBranch(existingEmployee.getBranch());
        }

        // Find and set department
        if (employee.getDepartment() != null && employee.getDepartment().getId() != null) {
            DepartmentMaster department = departmentMasterRepository.findById(employee.getDepartment().getId())
                    .orElseThrow(() -> new RuntimeException("Department Not Found"));
            employee.setDepartment(department);
        } else {
            employee.setDepartment(existingEmployee.getDepartment());
        }

        // Retain active status
        employee.setActive(existingEmployee.isActive());

        // Save and return the updated employee
        Employee savedEmployee = employeeRepository.save(employee);

        log.info("SUCCESS → Employee Saved | id={} name={}", savedEmployee.getId(), savedEmployee.getName());

        return savedEmployee;
    }

    @Override
    @Transactional
    public ApiResponse<Employee> updateEmployeeDetails(Integer employeeId, String name, String email, String mobile,
                                                       Integer branchId, Integer departmentId) {
        log.info("API CALL → Update Employee Details | id={}", employeeId);

        Optional<Employee> employeeOptional = employeeRepository.findById(employeeId);
        if (employeeOptional.isEmpty()) {
            log.error("FAILED → Update Employee Details | id={} reason=Employee Not Found", employeeId);
            return ResponseUtils.createNotFoundResponse("Employee not found", HttpStatus.NOT_FOUND.value());
        }

        Employee employee = employeeOptional.get();
        Map<String, Boolean> changedFields = new HashMap<>();

        // Update name
        if (name != null && !name.isEmpty() && !name.equals(employee.getName())) {
            employee.setName(name);
            changedFields.put("name", true);
        }

        // Update email
        if (email != null && !email.isEmpty() && !email.equals(employee.getEmail())) {
            employee.setEmail(email);
            changedFields.put("email", true);
        }

        // Update mobile
        if (mobile != null && !mobile.isEmpty() && !mobile.equals(employee.getMobile())) {
            employee.setMobile(mobile);
            changedFields.put("mobile", true);
        }

        // Update branch
        if (branchId != null && (employee.getBranch() == null || !branchId.equals(employee.getBranch().getId()))) {
            Optional<BranchMaster> branchOptional = branchMasterRepository.findById(branchId);
            if (branchOptional.isEmpty()) {
                log.error("FAILED → Update Employee Details | id={} reason=Branch Not Found", employeeId);
                return ResponseUtils.createNotFoundResponse("Branch not found", HttpStatus.NOT_FOUND.value());
            }
            employee.setBranch(branchOptional.get());
            changedFields.put("branch", true);
        }

        // Update department
        if (departmentId != null && (employee.getDepartment() == null || !departmentId.equals(employee.getDepartment().getId()))) {
            Optional<DepartmentMaster> departmentOptional = departmentMasterRepository.findById(departmentId);
            if (departmentOptional.isEmpty()) {
                log.error("FAILED → Update Employee Details | id={} reason=Department Not Found", employeeId);
                return ResponseUtils.createNotFoundResponse("Department not found", HttpStatus.NOT_FOUND.value());
            }
            employee.setDepartment(departmentOptional.get());
            changedFields.put("department", true);
        }

        // Update the timestamp
        employee.setUpdatedOn(new java.sql.Timestamp(System.currentTimeMillis()));

        // Save the updated employee details
        employee = employeeRepository.save(employee);

        // Create notification if any field changed
        if (!changedFields.isEmpty()) {
            // Ensure valid boolean values
            for (Map.Entry<String, Boolean> entry : changedFields.entrySet()) {
                if (entry.getValue() == null) {
                    entry.setValue(false);
                }
            }
            notificationService.createEmployeeUpdateNotification(employee, "PROFILE_UPDATE", changedFields);
        }

        log.info("SUCCESS → Employee Details Updated | id={} name={}", employeeId, employee.getName());

        return ResponseUtils.createSuccessResponse(employee, new TypeReference<>() {});
    }


    @Override
    public Employee findByEmail(String email) {
        log.info("API CALL → Find Employee By Email | email={}", email);

        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found with email: " + email));
    }

    @Override
    public void deleteByIdEmployee(Integer id) {
        log.info("API CALL → Delete Employee | id={}", id);

        employeeRepository.deleteById(id);

        log.info("SUCCESS → Employee Deleted | id={}", id);
    }

    @Override
    public List<Employee> findAllEmployee() {
        log.info("API CALL → Get All Employees");

        List<Employee> employees = employeeRepository.findEmployeesOrdered();
        log.info("SUCCESS → Retrieved {} employees", employees.size());

        return employees;
    }


    @Override
    public Employee findById(Integer id) {
        log.info("API CALL → Get Employee By ID | id={}", id);

        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found", "Id", id));
    }


    @Override
    public Employee updateEmployeeStatus(Integer id, boolean isActive, HttpServletRequest request) {
        log.info("API CALL → Employee Status Update | id={} newStatus={}", id, isActive);

        // 🔹 Try to find the employee
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> {
                    // 🔹 Failure log
                    log.error("FAILED → Status Update | id={} reason=Employee Not Found", id);

                    auditLogUtil.logAction(
                            currentUser.getCurrentEmployeeOrThrow(),  // employee performing action
                            "Employees",                             // form/page
                            "StatusUpdate",                          // activity
                            "Failure",                               // status
                            id,                                      // entityId
                            null,                                    // entityName (not found)
                            id,                                      // uniqueId
                            Map.of("error", "Employee not found"),   // details
                            request
                    );
                    return new ResourceNotFoundException("Employee", "id", id);
                });

        // 🔹 Capture previous data
        Map<String, Object> previousData = Map.of(
                "isActive", employee.isActive()
        );

        // 🔹 Update fields
        employee.setActive(isActive);
        employee.setUpdatedOn(Helper.getCurrentTimeStamp());

        Employee updatedEmployee = employeeRepository.save(employee);

        // 🔹 Send email notification
        String status = isActive ? "Activated" : "Deactivated";
        emailService.sendStatusUpdateNotification(updatedEmployee, isActive);

        // 🔹 Create app notification
        notificationService.createEmployeeUpdateNotification(updatedEmployee, "STATUS_CHANGE", null);

        // 🔹 Audit log (success)
        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),    // employee performing action
                "Employees",                               // form/page
                "StatusUpdate",                             // activity
                "Success",                                  // status
                updatedEmployee.getId(),                    // entityId
                updatedEmployee.getName(),                  // entityName
                updatedEmployee.getId(),                    // uniqueId
                previousData,                               // previous data
                request
        );

        log.info("SUCCESS → Employee Status Updated | id={} name={} oldStatus={} newStatus={}",
                id, updatedEmployee.getName(), !isActive, isActive);

        return updatedEmployee;
    }

    @Override
    @Transactional
    public void updateLanguageOnly(Integer employeeId, Long languageId) {
        log.info("API CALL → Update Employee Language | employeeId={} languageId={}", employeeId, languageId);

        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee != null) {
            LanguageMaster language = languageMasterRepository.findById(languageId).orElse(null);
            if (language != null) {
                employee.setLanguage(language);
                employeeRepository.save(employee);
                log.info("SUCCESS → Employee Language Updated | employeeId={} language={}",
                        employeeId, language.getName());
            }
        }
    }


    @Override
    public Employee updateEmployeeRoleByName(Integer id, String roleName, HttpServletRequest request) {
        log.info("API CALL → Update Employee Role | id={} roleName={}", id, roleName);

        try {
            // ✅ Get the current authenticated user
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Employee currentEmployee = findByEmail(currentUser.getUsername());

            if (currentEmployee == null) {
                throw new RuntimeException("Current user not found");
            }

            // ✅ Fetch existing employee (before update) to capture old data
            Optional<Employee> optionalEmployee = employeeRepository.findById(id);

            if (optionalEmployee.isEmpty()) {
                // 🔴 Audit failure log
                auditLogUtil.logAction(
                        currentEmployee,
                        "Pending Users",
                        "Assign Role",
                        "Failure",
                        id,
                        null,
                        id,
                        Map.of("error", "Employee with ID " + id + " not found", "roleName", roleName),
                        request
                );
                throw new RuntimeException("Employee with ID " + id + " not found.");
            }

            Employee employee = optionalEmployee.get();

            // ✅ Capture previous data (before role update)
            Map<String, Object> previousData = Map.of(
                    "employeeName", employee.getName(),
                    "employeeEmail", employee.getEmail(),
                    "previousRole", employee.getRole() != null ? employee.getRole().getRole() : "No Role",
                    "newRole", roleName,
                    "branch", employee.getBranch() != null ? employee.getBranch().getName() : null,
                    "department", employee.getDepartment() != null ? employee.getDepartment().getName() : null
            );

            Optional<RoleMaster> optionalRole = roleMasterRepository.findByRole(roleName);

            if (optionalRole.isEmpty()) {
                // 🔴 Audit failure log
                auditLogUtil.logAction(
                        currentEmployee,
                        "Pending Users",
                        "Assign Role",
                        "Failure",
                        id,
                        employee.getName(),
                        id,
                        Map.of("error", "Role with Name " + roleName + " not found", "employeeName", employee.getName()),
                        request
                );
                throw new RuntimeException("Role with Name " + roleName + " not found.");
            }

            Boolean flag = false;
            if (employee.getRole() != null)
                flag = true;

            RoleMaster newRole = optionalRole.get();

            // Check if the new role is 'admin' and if the employee's branch already has an admin
            if (newRole.getRole().equalsIgnoreCase("ADMIN")) {
                Optional<Employee> existingAdmin = employeeRepository.findSingleByRole(newRole);

                if (existingAdmin.isPresent() && !existingAdmin.get().getId().equals(employee.getId())) {
                    // 🔴 Audit failure log
                    auditLogUtil.logAction(
                            currentEmployee,
                            "Pending Users",
                            "Assign Role",
                            "Failure",
                            id,
                            employee.getName(),
                            id,
                            Map.of(
                                    "error", "There is already a system-wide admin assigned",
                                    "existingAdmin", existingAdmin.get().getName(),
                                    "requestedRole", roleName
                            ),
                            request
                    );
                    throw new RuntimeException("There is already a system-wide admin assigned.");
                }
            }

            if (newRole.getRole().equalsIgnoreCase("BRANCH ADMIN")) {
                Optional<Employee> existingAdmin = employeeRepository.findByRoleAndBranch(newRole, employee.getBranch());
                // Commented validation as per your code
            }

            if (newRole.getRole().equalsIgnoreCase("DEPARTMENT ADMIN")) {
                Optional<Employee> existingAdmin = employeeRepository.findByRoleAndDepartment(newRole, employee.getDepartment());
                // Commented validation as per your code
            }

            // Assign the role to the employee and update the employee
            employee.setRole(newRole);
            employee.setActive(true);
            employee.setUpdatedOn(Helper.getCurrentTimeStamp());

            // Save the updated employee with the new role
            if (!flag)
                employeeRepository.save(employee);

            updateEmployeeRole(employee, newRole, currentEmployee);

            // ✅ Audit success log (after successful update)
            auditLogUtil.logAction(
                    currentEmployee,
                    "Pending Users",
                    "Assign Role",
                    "Success",
                    id,
                    employee.getName(),
                    id,
                    previousData,
                    request
            );

            notificationService.createRoleAssignmentNotification(employee, newRole, currentEmployee);

            log.info("SUCCESS → Employee Role Updated | id={} name={} newRole={}", id, employee.getName(), roleName);

            return employee;

        } catch (Exception e) {
            log.error("ERROR → Update Employee Role | id={} reason={}", id, e.getMessage());
            e.printStackTrace();

            // 🔴 Audit error log (for unexpected exceptions)
            auditLogUtil.logAction(
                    null, // currentEmployee might not be available in catch block
                    "Pending Users",
                    "Assign Role",
                    "Error",
                    id,
                    null,
                    id,
                    Map.of("exception", e.getMessage(), "roleName", roleName),
                    request
            );

            throw e; // Re-throw the exception to maintain original behavior
        }
    }

    @Override
    public void updateEmployeeRoleByEmail(String email, Integer roleId) {
        log.info("API CALL → Update Employee Role By Email | email={} roleId={}", email, roleId);

        Optional<Employee> optionalEmployee = employeeRepository.findByEmail(email);

        if (optionalEmployee.isEmpty()) {
            throw new RuntimeException("Employee with email " + email + " not found.");
        }

        Employee employee = optionalEmployee.get();
        Optional<RoleMaster> optionalRole = roleMasterRepository.findById(roleId);

        if (optionalRole.isEmpty()) {
            throw new RuntimeException("Role with ID " + roleId + " not found.");
        }

        RoleMaster newRole = optionalRole.get();

        // Check if the new role is 'admin' and if the employee's branch already has an admin
        if (newRole.getRole().equalsIgnoreCase("ADMIN")) {
            Optional<Employee> existingAdmin = employeeRepository.findByRoleAndBranch(newRole, employee.getBranch());
//            if (existingAdmin.isPresent() && !existingAdmin.get().getId().equals(employee.getId())) {
//                throw new RuntimeException("There is already an admin assigned to this ");
//            }
        }
        if (newRole.getRole().equalsIgnoreCase("BRANCH ADMIN")) {
            Optional<Employee> existingAdmin = employeeRepository.findByRoleAndBranch(newRole, employee.getBranch());
//            if (existingAdmin.isPresent() && !existingAdmin.get().getId().equals(employee.getId())) {
//                throw new RuntimeException("There is already an Branch admin assigned to this branch.");
//            }
        }
        if (newRole.getRole().equalsIgnoreCase("DEPARTMENT ADMIN")) {
            Optional<Employee> existingAdmin = employeeRepository.findByRoleAndDepartment(newRole, employee.getDepartment());
//            if (existingAdmin.isPresent() && !existingAdmin.get().getId().equals(employee.getId())) {
//                throw new RuntimeException("There is already a Department admin assigned to this department.");
//            }
        }


        // Assign the role to the employee and update the employee
        employee.setRole(newRole);
        employee.setUpdatedOn(Helper.getCurrentTimeStamp());

        // Save the updated employee with the new role
        employeeRepository.save(employee);

        log.info("SUCCESS → Employee Role Updated By Email | email={} newRole={}", email, newRole.getRole());
    }

    @Override
    public List<EmployeeDTO> findEmployeesByBranch(BranchMaster branch) {
        log.info("API CALL → Get Employees By Branch | branchId={} branchName={}",
                branch.getId(), branch.getName());

        List<Employee> employees = employeeRepository.findByBranchOrdered(branch);
        log.info("SUCCESS → Retrieved {} employees for branch {}", employees.size(), branch.getName());

        return employees.stream()
                .map(this::mapToDTO)
                .toList();
    }


    @Override
    public List<Employee> getEmployeesByRoleIsNullById(Integer id) {
        log.info("API CALL → Get Employees By Role Is Null By ID | id={}", id);
        return employeeRepository.findByIdAndRoleIsNull(id);
    }

    @Override
    public List<Employee> getEmployeesByRoleIsNull() {
        log.info("API CALL → Get Employees With Null Role");

        List<Employee> employees = employeeRepository.findByRoleIsNullOrdered();
        log.info("SUCCESS → Retrieved {} employees with null role", employees.size());
        return employees;
    }

    @Override
    public List<Employee> getAllWithoutNullRole() {
        log.info("API CALL → Get All Employees Without Null Role");

        List<Employee> employees = employeeRepository.findAllByRoleIsNotNull();
        log.info("SUCCESS → Retrieved {} employees with non-null role", employees.size());
        return employees;
    }

    @Override
    public List<Employee> findAllByRoleIsNotNullAndDepartment(DepartmentMaster department) {
        log.info("API CALL → Get Employees With Non-Null Role By Department | departmentId={}", department.getId());
        return employeeRepository.findAllByRoleIsNotNullAndDepartment(department);
    }

    @Override
    public List<Employee> findAllByRoleIsNotNullAndBranch(BranchMaster branchId) {
        log.info("API CALL → Get Employees With Non-Null Role By Branch | branchId={}", branchId.getId());
        return employeeRepository.findAllByRoleIsNotNullAndBranch(branchId);
    }

    @Override
    public long countEmployeesByRoleNull() {
        log.info("API CALL → Count Employees With Null Role");

        long count = employeeRepository.countByRoleIsNull();
        log.info("SUCCESS → Counted {} employees with null role", count);
        return count;
    }

    @Override
    public long countEmployeesByRoleNotNull() {
        log.info("API CALL → Count Employees With Non-Null Role");

        long count = employeeRepository.countByRoleIsNotNull();
        log.info("SUCCESS → Counted {} employees with non-null role", count);
        return count;
    }

    @Override
    public long countEmployeesByRole(String roleName) {
        log.info("API CALL → Count Employees By Role | roleName={}", roleName);

        RoleMaster role = roleMasterRepository.findByRole(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        long count = employeeRepository.countByRole(role);
        log.info("SUCCESS → Counted {} employees with role {}", count, roleName);
        return count;
    }

    @Override
    public List<Employee> findEmployeesByRole(String roleName) {
        log.info("API CALL → Get Employees By Role | roleName={}", roleName);

        RoleMaster role = roleMasterRepository.findByRole(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        List<Employee> employees = employeeRepository.findByRole(role);
        log.info("SUCCESS → Retrieved {} employees with role {}", employees.size(), roleName);
        return employees;
    }

    @Override
    public void changePassword(String email, String currentPassword, String newPassword) {
        log.info("API CALL → Change Password | email={}", email);

        Employee employee = findByEmail(email);
        if (employee == null) {
            throw new RuntimeException("Employee not found with email: " + email);
        }

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, employee.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Set the new password (make sure to encode it)
        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);

        log.info("SUCCESS → Password Changed | email={}", email);
    }

    public EmployeeRole updateEmployeeRole(Employee employee, RoleMaster role, Employee currentUser) {
        log.debug("Updating Employee Role | employeeId={} role={}", employee.getId(), role.getRole());

        // Validate the role name
//        if (roleName == null || roleName.isEmpty()) {
//            throw new IllegalArgumentException("Role name must not be null or empty.");
//        }

        // Find the role by name
//        RoleMaster role = roleMasterRepository.findByRole(roleName).get();
        EmployeeRole employeeRole = new EmployeeRole();
//        Employee employee;
//        Integer employeeId = Integer.parseInt(identifier);
//        employee = employeeService.findById(employeeId);

//        if (employee == null) {
//            throw new ResourceNotFoundException("Employee not found for identifier: " + identifier);
//        }
        if (role != null) {
            if (employee.getRole() == null) {
                employee.setRole(role);
            }
            // Create or update the EmployeeRole record
            employeeRole.setEmpId(employee);
            employeeRole.setRoleId(role);
            employeeRole.setActive(true); // Assume active by default
            employeeRole.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
            employeeRole.setUpdatedBy(currentUser);

            if (employeeRole.getCreatedOn() == null) {
                employeeRole.setCreatedOn(new Timestamp(System.currentTimeMillis()));
                employeeRole.setCreatedBy(currentUser);
            }
        } else {
            throw new ResourceNotFoundException("Role not found for Role: " + role.getRole());
        }
        return employeeRoleRepository.save(employeeRole);

    }

    @Override
    public List<Employee> findEmployeesWithNullRoleByBranch(BranchMaster branch) {
        log.info("API CALL → Get Employees With Null Role By Branch | branchId={}", branch.getId());
        return employeeRepository.findByRoleIsNullAndBranchOrdered(branch);
    }

    @Override
    public EmployeeDTO mapToDTO(Employee employee) {
        log.debug("Mapping Employee to DTO | employeeId={} name={}", employee.getId(), employee.getName());

        if (employee == null) return null;

        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setBranch(employee.getBranch());
        dto.setMobile(employee.getMobile());
        dto.setActive(employee.isActive());
        dto.setCreatedOn(employee.getCreatedOn());
        dto.setUpdatedOn(employee.getUpdatedOn());
        dto.setDepartment(employee.getDepartment());
        dto.setRole(employee.getRole());

        // Handle createdBy and updatedBy with limited mapping or null-check
        if (employee.getCreatedBy() != null) {
            EmployeeDTO createdByDTO = new EmployeeDTO();
            createdByDTO.setId(employee.getCreatedBy().getId());
            createdByDTO.setName(employee.getCreatedBy().getName());
            dto.setCreatedBy(createdByDTO);
        }

        if (employee.getUpdatedBy() != null) {
            EmployeeDTO updatedByDTO = new EmployeeDTO();
            updatedByDTO.setId(employee.getUpdatedBy().getId());
            updatedByDTO.setName(employee.getUpdatedBy().getName());
            dto.setUpdatedBy(updatedByDTO);
        }

        return dto;
    }



    @Override
    public List<Employee> findEmployeesWithNullRole() {
        log.info("API CALL → Get Employees With Null Role");
        return employeeRepository.findByRoleIsNullOrdered();
    }

    @Override
    public ApiResponse<List<EmployeeDTO>> findEmployeesByDepartment(DepartmentMaster department) {
        log.info("API CALL → Get Employees By Department | departmentId={} departmentName={}",
                department.getId(), department.getName());

        if (department == null) {
            throw new IllegalArgumentException("Department cannot be null");
        }

        // Fetch the list of employees
        List<Employee> employeeList = employeeRepository.findByDepartmentOrdered(department);

        // Map Employee entities to EmployeeDTOs
        List<EmployeeDTO> resp = employeeList.stream()
                .map(this::mapToDTO) // Convert each Employee to EmployeeDTO
                .toList();

        log.info("SUCCESS → Retrieved {} employees for department {}", resp.size(), department.getName());

        // Return the success response
        return ResponseUtils.createSuccessResponse(resp, new TypeReference<>() {});
    }



    @Override
    public ApiResponse<List<Employee>>findEmployeeByCreatedByEmp(Employee employee){
        log.info("API CALL → Get Employees By Creator | creatorId={}", employee.getId());

        if (employee == null){
            throw new IllegalArgumentException("Employee Id Cannot be null");
        }
        List<Employee>employeeList = employeeRepository.findEmployeesByCreatedBy(employee);

        log.info("SUCCESS → Retrieved {} employees created by employee {}", employeeList.size(), employee.getId());

        return ResponseUtils.createSuccessResponse(employeeList, new TypeReference<>() {
        });
    }

    @Override
    public List<Employee> findEmployeesWithNullRoleByDepartment(DepartmentMaster department) {
        log.info("API CALL → Get Employees With Null Role By Department | departmentId={}", department.getId());
        return employeeRepository.findByRoleIsNullAndDepartmentOrdered(department);
    }

    @Override
    public ApiResponse<FileResponse> getFilteredEmployeesApiResponse(EmployeeFilterRequest employeeFilterRequest) {
        log.info("API CALL → Get Filtered Employees Report | docType={} branchId={} departmentId={}",
                employeeFilterRequest.getDocType(),
                employeeFilterRequest.getDepartmentMasterBranchId(),
                employeeFilterRequest.getDepartmentMasterId());

        List<EmployeeResponse> employeeResponses = employeeRepository.findByFilters(
                        employeeFilterRequest.getDepartmentMasterBranchId(),
                        employeeFilterRequest.getDepartmentMasterId(),
                        employeeFilterRequest.getStatus(),
                        employeeFilterRequest.getStartDate(),
                        employeeFilterRequest.getEndDate()
                ).stream()
                .map(this::mapToEmployeeResponse) // Ensure mapToEmployeeResponse is defined in this class
                .collect(Collectors.toList());

        log.info("Filtered {} employees for report", employeeResponses.size());

        try {
            FileResponse fileResponse = generateDocument(employeeResponses, employeeFilterRequest);
            log.info("SUCCESS → Generated {} report with {} employees",
                    employeeFilterRequest.getDocType(), employeeResponses.size());

            return ResponseUtils.createSuccessResponse(fileResponse, new TypeReference<FileResponse>() {});
        } catch (IllegalArgumentException e) {
            log.error("FAILED → Generate Report | reason={}", e.getMessage());
            return ResponseUtils.createFailureResponse(null, new TypeReference<FileResponse>() {}, e.getMessage(), HttpStatus.BAD_REQUEST.value());
        } catch (Exception e) {
            log.error("FAILED → Generate Report | reason={}", e.getMessage());
            return ResponseUtils.createFailureResponse(null, new TypeReference<FileResponse>() {}, "Error generating document: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private EmployeeResponse mapToEmployeeResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId() != null ? employee.getId() : -1,
                employee.getName() != null ? employee.getName() : "No Data",
                employee.getBranch() != null && employee.getBranch().getName() != null ? employee.getBranch().getName() : "No Data",
                employee.getDepartment() != null && employee.getDepartment().getName() != null ? employee.getDepartment().getName() : "No Data",
                employee.getRole() != null && employee.getRole().getRole() != null ? employee.getRole().getRole() : "No Data",
                employee.isActive() ? "Active" : "Inactive",
                employee.getCreatedOn(),
                employee.getMobile() != null ? employee.getMobile() : "No Data",
                employee.getEmail() != null ? employee.getEmail() : "No Data"
        );
    }

    private FileResponse generateDocument(List<EmployeeResponse> employeeResponses, EmployeeFilterRequest request) throws Exception {
        String docType = request.getDocType();
        byte[] fileContent;
        String fileName;

        // Resolve branchName and departmentName based on request's IDs
        String branchName = resolveBranchName(request.getDepartmentMasterBranchId());
        String departmentName = resolveDepartmentName(request.getDepartmentMasterId());

        if ("PDF".equalsIgnoreCase(docType)) {


            // Convert Timestamp to String
            String formattedStartDate = request.getStartDate() != null
                    ? new SimpleDateFormat("yyyy-MM-dd").format(request.getStartDate())
                    : null;

            String formattedEndDate = request.getEndDate() != null
                    ? new SimpleDateFormat("yyyy-MM-dd").format(request.getEndDate())
                    : null;

            fileContent = pdfGenerator.generatePdf(
                    employeeResponses,
                    branchName,
                    departmentName,
                    resolveStatusNullable(request.getStatus()),
                    pdfGenerator.formatDate(formattedStartDate, "yyyy-MM-dd", "dd-MM-yyyy"),
                    pdfGenerator.formatDate(formattedEndDate, "yyyy-MM-dd", "dd-MM-yyyy")
            );

            // Generate dynamic file name
            fileName = pdfGenerator.getDynamicFileName(branchName, departmentName);
            System.out.println("service " + fileName);
        } else if ("Excel".equalsIgnoreCase(docType)) {
            EmpExcelGenerator excelGenerator = new EmpExcelGenerator();
            fileContent = excelGenerator.generateExcel(employeeResponses);

            String fromDate = resolveFromDate(request.getStartDate());
            String toDate = resolveToDate(request.getEndDate());

            fileName =excelGenerator.getDynamicFileName(branchName, departmentName, fromDate, toDate);
        } else {
            throw new IllegalArgumentException("Invalid document type. Supported types: 'PDF' or 'Excel'.");
        }

        FileResponse fileResponse = new FileResponse();
        fileResponse.setFileName(fileName);
        fileResponse.setFileContent(fileContent);
        return fileResponse;
    }

    private String resolveStatusNullable(Boolean status) {
        if (status == null) {
            return "All Status";
        }
        return status ? "Active" : "Inactive";
    }


    // Helper methods to resolve names
    private String resolveBranchName(Integer branchId) {
        if (branchId == null) {
            return "All Branches"; // Default value if no branch is selected
        }
        return branchMasterRepository.findById(branchId)
                .map(BranchMaster::getName)
                .orElse("Unknown Branch");
    }

    private String resolveDepartmentName(Integer departmentId) {
        if (departmentId == null) {
            return "All Departments"; // Default value if no department is selected
        }
        return departmentMasterRepository.findById(departmentId)
                .map(DepartmentMaster::getName)
                .orElse("Unknown Department");
    }

    private String resolveFromDate(Timestamp timestamp) {
        if (timestamp == null) {
            return null; // or a default date if needed
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        return dateFormat.format(timestamp);
    }

    private String resolveToDate(Timestamp timestamp) {
        if (timestamp == null) {
            return null; // or a default date if needed
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        return dateFormat.format(timestamp);
    }


    private String resolveStatus(Boolean status) {
        return status != null && status ? "Active" : "Inactive";
    }



    //=========================Profile image=========================

    @Override
    public Optional<ProfileImage> findByEmployee(Employee employee) {
        log.debug("Finding Profile Image | employeeId={}", employee.getId());
        return profileImageRepository.findByEmployee(employee);
    }

    @Override
    public void saveProfileImage(ProfileImage profileImage) {
        log.info("API CALL → Save Profile Image | employeeId={}", profileImage.getEmployee().getId());
        profileImageRepository.save(profileImage);
        log.info("SUCCESS → Profile Image Saved | employeeId={}", profileImage.getEmployee().getId());
    }

    @Override
    @Transactional
    public void saveProfileImage(Employee employee, MultipartFile file) {
        log.info("API CALL → Save Profile Image With File | employeeId={} fileName={}",
                employee.getId(), file.getOriginalFilename());

        try {
            // Ensure the "ProfileStorage" directory exists inside the "documentStoragePath"
            Path profileStoragePath = Paths.get(documentStoragePath, "ProfileStorage");
            Files.createDirectories(profileStoragePath);

            // Construct the file name and full path
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = profileStoragePath.resolve(fileName);

            // Save the file to the directory
            Files.write(filePath, file.getBytes());

            // Construct the full path for the database
            String fullPath = filePath.toAbsolutePath().toString();

            // Handle ProfileImage entity
            ProfileImage profileImage = employee.getProfileImage();
            if (profileImage == null) {
                profileImage = new ProfileImage();
                profileImage.setEmployee(employee);
            }
            profileImage.setDpImageSrc(fullPath); // Update the full path
            profileImage.setDpImageName(fileName); // Save the file name as well

            // Save profile image details in the database
            profileImageRepository.save(profileImage);

            log.info("SUCCESS → Profile Image Saved | employeeId={} filePath={}", employee.getId(), fullPath);

        } catch (IOException e) {
            log.error("FAILED → Save Profile Image | employeeId={} reason={}", employee.getId(), e.getMessage());
            throw new RuntimeException("Error saving profile image", e);
        }
    }


    @Override
    @Transactional
    public Employee updateProfile(Employee employee, Integer loggedInEmployeeId) {
        log.info("API CALL → Update Profile | employeeId={} updatedBy={}", employee.getId(), loggedInEmployeeId);

        Employee existingEmployee = employeeRepository.findById(employee.getId())
                .orElseThrow(() -> new RuntimeException("Employee Not Found"));

        // Update the other fields
        existingEmployee.setMobile(employee.getMobile());
        existingEmployee.setName(employee.getName());
        existingEmployee.setUpdatedOn(Helper.getCurrentTimeStamp());

        // Set the employee performing the update
        Employee updatedBy = employeeRepository.findById(loggedInEmployeeId)
                .orElseThrow(() -> new RuntimeException("Logged-in Employee Not Found"));
        existingEmployee.setUpdatedBy(updatedBy);

        Employee savedEmployee = employeeRepository.save(existingEmployee);

        log.info("SUCCESS → Profile Updated | employeeId={} name={}", savedEmployee.getId(), savedEmployee.getName());

        return savedEmployee;
    }

    @Override
    public ApiResponse<Employee> switchEmployeeRole(String employeeIdentifier, String targetRoleName, Employee currentEmployee) {
        log.info("API CALL → Switch Employee Role | employeeIdentifier={} targetRole={}",
                employeeIdentifier, targetRoleName);

        // Find the employee
        Integer employeeId = Integer.parseInt(employeeIdentifier);
        Employee employee = findById(employeeId);

        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found for identifier: " + employeeIdentifier);
        }

        // Find all roles assigned to this employee
        List<EmployeeRole> existingEmployeeRoles = employeeRoleRepository.findAllByEmpId(employee);

        if (existingEmployeeRoles.isEmpty()) {
            throw new ResourceNotFoundException("No roles found for employee");
        }

        // Find the target role
        EmployeeRole targetEmployeeRole = existingEmployeeRoles.stream()
                .filter(empRole -> empRole.getRoleId().getRole().equals(targetRoleName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Target role " + targetRoleName + " is not assigned to this employee"));

        // Check if the role is active before proceeding with the switch
        if (!targetEmployeeRole.isActive()) {
            throw new IllegalStateException("Cannot switch to an inactive role: " + targetRoleName);
        }

        // Update the employee's role
        RoleMaster targetRole = targetEmployeeRole.getRoleId();
        employee.setRole(targetRole);

        // Save the updated employee
        Employee emp = employeeRepository.save(employee);

        log.info("SUCCESS → Employee Role Switched | employeeId={} newRole={}", employeeId, targetRoleName);

        return ResponseUtils.createSuccessResponse(emp, new TypeReference<>() {});
    }



    @Override
    public List<Employee> findByDepartmentAndRole(Integer departmentId, String roleName) {
        log.info("API CALL → Find Employees By Department And Role | departmentId={} roleName={}",
                departmentId, roleName);

        System.out.println("\n====== Finding Employees by Department and Role ======");
        System.out.println("Department ID: " + departmentId);
        System.out.println("Role Name: " + roleName);

        try {
            List<Employee> employees = employeeRepository.findByDepartmentIdAndRoleRole(departmentId, roleName);
            System.out.println("Query executed successfully");
            System.out.println("Found " + employees.size() + " employees");

            if (employees.isEmpty()) {
                System.out.println("WARNING: No employees found with role '" + roleName +
                        "' in department ID " + departmentId);
            } else {
                System.out.println("\nFound employees:");
                for (Employee emp : employees) {
                    System.out.println("- " + emp.getName() +
                            " (ID: " + emp.getId() +
                            ", Role: " + emp.getRole().getRole() + ")");
                }
            }

            log.info("SUCCESS → Found {} employees with role '{}' in department ID {}",
                    employees.size(), roleName, departmentId);

            return employees;
        } catch (Exception e) {
            log.error("FAILED → Find Employees By Department And Role | departmentId={} roleName={} reason={}",
                    departmentId, roleName, e.getMessage());
            System.out.println("ERROR in findByDepartmentAndRole: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<StatusCountByYearDto> getStatusCountsPerYear() {
        log.info("API CALL → Get Status Counts Per Year");

        List<StatusCountByYearDto> counts = employeeRepository.getStatusCountGroupedByYear();

        log.info("SUCCESS → Retrieved status counts for {} years", counts.size());

        return counts;
    }

    @Override
    public Optional<Employee> findByEmailOrMobile(String identifier) {
        log.info("API CALL → Find Employee By Email or Mobile | identifier={}", identifier);

        Optional<Employee> employeeByEmail = employeeRepository.findByEmail(identifier);
        if (employeeByEmail.isPresent()) {
            return employeeByEmail;
        }

        Optional<Employee> employeeByMobile = employeeRepository.findByMobile(identifier);
        if (employeeByMobile.isPresent()) {
            return employeeByMobile;
        }

        log.info("No employee found with identifier: {}", identifier);
        return Optional.empty(); // user doesn't exist
    }


    @Override
    public void resetPassword(String email, String newPassword) {
        log.info("API CALL → Reset Password | email={}", email);

        Employee employee = findByEmail(email);
        if (employee == null) {
            throw new RuntimeException("Employee not found with email: " + email);
        }

        employee.setPassword(passwordEncoder.encode(newPassword));
        employee.setUpdatedOn(Helper.getCurrentTimeStamp());
        employeeRepository.save(employee);

        log.info("SUCCESS → Password Reset | email={}", email);
    }


    @Override
    public ApiResponse<List<EmployeeDTO>> getEmployeesOfCurrentUserBranchAndDepartment() {
        log.info("API CALL → Get Employees Of Current User Branch and Department");

        // ✅ Get logged-in employee
        Employee loggedInEmployee = currentUser.getCurrentEmployeeOrThrow();

        // ✅ Get branch & department from logged-in employee
        BranchMaster branch = loggedInEmployee.getBranch();
        DepartmentMaster department = loggedInEmployee.getDepartment();

        if (branch == null || department == null) {
            throw new IllegalStateException("Logged-in user has no branch or department assigned");
        }

        // ✅ Fetch employees
        List<Employee> employees =
                employeeRepository.findByBranchAndDepartment(
                        branch.getId(),
                        department.getId()
                );

        log.info("Found {} employees in current user's branch and department", employees.size());

        // ✅ Map to DTO
        List<EmployeeDTO> response = employees.stream()
                .map(this::mapToDTO)
                .toList();

        return ResponseUtils.createSuccessResponse(response, new TypeReference<>() {});
    }



}