package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.Employee;
import com.dmsBackend.entity.EmployeeRole;
import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.repository.EmployeeRepository;
import com.dmsBackend.repository.EmployeeRoleRepository;
import com.dmsBackend.repository.RoleMasterRepository;
import com.dmsBackend.response.AllEmployeeRoleResponse;
import com.dmsBackend.response.EmployeeRoleResponse;
import com.dmsBackend.service.EmployeeRoleService;
import com.dmsBackend.service.EmployeeService;
import com.dmsBackend.utils.AuditLogUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmployeeRoleServiceImpl implements EmployeeRoleService {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private RoleMasterRepository roleMasterRepository;

    @Autowired
    private EmployeeRoleRepository employeeRoleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AuditLogUtil auditLogUtil;

    // ======================= UPDATE EMPLOYEE ROLE =======================
    @Override
    public EmployeeRole updateEmployeeRole(Employee employee, RoleMaster role, Employee currentUser) {

        log.info("API CALL → Update Employee Role | employeeId={} role={}",
                employee.getId(), role != null ? role.getRole() : "null");

        if (role != null) {
            if (employee.getRole() == null) {
                employee.setRole(role);
            }

            EmployeeRole employeeRole = new EmployeeRole();
            employeeRole.setEmpId(employee);
            employeeRole.setRoleId(role);
            employeeRole.setActive(true);
            employeeRole.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
            employeeRole.setUpdatedBy(currentUser);

            if (employeeRole.getCreatedOn() == null) {
                employeeRole.setCreatedOn(new Timestamp(System.currentTimeMillis()));
                employeeRole.setCreatedBy(currentUser);
            }

            EmployeeRole savedRole = employeeRoleRepository.save(employeeRole);

            log.info("SUCCESS → Employee Role Updated | employeeId={} employeeName={} role={}",
                    employee.getId(), employee.getName(), role.getRole());

            return savedRole;
        } else {
            log.info("FAILED → Update Employee Role | employeeId={} reason=Role not found", employee.getId());
            throw new ResourceNotFoundException("Role not found for Role: " + role.getRole());
        }
    }

    // ======================= CREATE OR UPDATE EMPLOYEE ROLE =======================
    @Override
    @Transactional
    public EmployeeRole createOrUpdateEmployeeRole(Integer empId, Integer roleId, HttpServletRequest request) {

        log.info("API CALL → Create/Update Employee Role | employeeId={} roleId={}", empId, roleId);

        try {
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Employee currentEmployee = employeeService.findByEmail(currentUser.getUsername());

            if (currentEmployee == null) {
                log.info("FAILED → Create/Update Employee Role | employeeId={} reason=Current user not found", empId);

                auditLogUtil.logAction(
                        currentEmployee,
                        "ManageEmployeeRoles",
                        "Update Role",
                        "Failure",
                        empId,
                        null,
                        empId,
                        Map.of("error", "Current user not found", "roleId", roleId),
                        request
                );
                throw new RuntimeException("Current user not found");
            }

            Employee emp = employeeRepository.findById(empId)
                    .orElseThrow(() -> {
                        log.info("FAILED → Create/Update Employee Role | employeeId={} reason=Employee not found", empId);

                        auditLogUtil.logAction(
                                currentEmployee,
                                "ManageEmployeeRoles",
                                "Update Role",
                                "Failure",
                                empId,
                                null,
                                empId,
                                Map.of("error", "Employee not found with id " + empId, "roleId", roleId),
                                request
                        );
                        return new RuntimeException("Employee not found with id " + empId);
                    });

            RoleMaster role = roleMasterRepository.findById(roleId)
                    .orElseThrow(() -> {
                        log.info("FAILED → Create/Update Employee Role | employeeId={} roleId={} reason=Role not found",
                                empId, roleId);

                        auditLogUtil.logAction(
                                currentEmployee,
                                "ManageEmployeeRoles",
                                "Update Role",
                                "Failure",
                                empId,
                                emp.getName(),
                                empId,
                                Map.of("error", "Role not found with id " + roleId, "employeeName", emp.getName()),
                                request
                        );
                        return new RuntimeException("Role not found with id " + roleId);
                    });

            List<EmployeeRole> allExistingRoles = employeeRoleRepository.findByEmpId_Id(empId);
            List<String> existingRoleNames = allExistingRoles.stream()
                    .filter(EmployeeRole::isActive)
                    .map(er -> er.getRoleId().getRole())
                    .toList();

            Optional<EmployeeRole> existingEmployeeRole = employeeRoleRepository.findByEmpId_IdAndRoleId_Id(empId, roleId);

            Map<String, Object> previousData = Map.of(
                    "name", emp.getName(),
                    "email", emp.getEmail(),
                    "previousRole", existingRoleNames,
                    "branch", emp.getBranch() != null ? emp.getBranch().getName() : null,
                    "department", emp.getDepartment() != null ? emp.getDepartment().getName() : null
            );

            EmployeeRole result = existingEmployeeRole
                    .map(existing -> {
                        log.debug("Updating existing role for employee {}: {} -> {}",
                                emp.getName(), existing.getRoleId().getRole(), role.getRole());

                        existing.setActive(true);
                        existing.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
                        existing.setUpdatedBy(currentEmployee);
                        return employeeRoleRepository.save(existing);
                    })
                    .orElseGet(() -> {
                        log.debug("Creating new role for employee {}: {}", emp.getName(), role.getRole());

                        EmployeeRole newRole = new EmployeeRole();
                        newRole.setEmpId(emp);
                        newRole.setRoleId(role);
                        newRole.setActive(true);
                        newRole.setCreatedOn(new Timestamp(System.currentTimeMillis()));
                        newRole.setCreatedBy(currentEmployee);
                        return employeeRoleRepository.save(newRole);
                    });

            List<EmployeeRole> updatedRoles = employeeRoleRepository.findByEmpId_Id(empId);
            List<String> updatedRoleNames = updatedRoles.stream()
                    .filter(EmployeeRole::isActive)
                    .map(er -> er.getRoleId().getRole())
                    .collect(Collectors.toList());

            Map<String, Object> finalAuditData = new HashMap<>(previousData);
            finalAuditData.put("newRoles", updatedRoleNames);

            auditLogUtil.logAction(
                    currentEmployee,
                    "ManageEmployeeRoles",
                    "Update Role",
                    "Success",
                    empId,
                    emp.getName(),
                    empId,
                    finalAuditData,
                    request
            );

            log.info("SUCCESS → Employee Role Created/Updated | employeeId={} employeeName={} role={}",
                    empId, emp.getName(), role.getRole());

            return result;

        } catch (Exception e) {
            log.error("FAILED → Create/Update Employee Role | employeeId={} roleId={} error={}",
                    empId, roleId, e.getMessage(), e);

            auditLogUtil.logAction(
                    null,
                    "ManageEmployeeRoles",
                    "Update Role",
                    "Error",
                    empId,
                    null,
                    empId,
                    Map.of("exception", e.getMessage(), "roleId", roleId),
                    request
            );

            throw e;
        }
    }

    // ======================= FIND ALL =======================
    @Override
    public List<EmployeeRole> findAll() {

        log.info("API CALL → Get All Employee Roles");

        List<EmployeeRole> allRoles = employeeRoleRepository.findAll();

        log.info("SUCCESS → Retrieved {} Employee Roles", allRoles.size());

        return allRoles;
    }

    // ======================= CHANGE ROLE STATUS =======================
    @Transactional
    public String changeRoleStatus(Integer empId, Integer roleId, boolean isActive,
                                   Employee updatedByEmployee, HttpServletRequest request) {

        log.info("API CALL → Change Role Status | employeeId={} roleId={} newStatus={}",
                empId, roleId, isActive);

        try {
            Employee currentEmployee = updatedByEmployee;
            if (currentEmployee == null) {
                User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                currentEmployee = employeeService.findByEmail(currentUser.getUsername());

                if (currentEmployee == null) {
                    log.info("FAILED → Change Role Status | employeeId={} reason=Current user not found", empId);

                    auditLogUtil.logAction(
                            null,
                            "ManageEmployeeRoles",
                            "Update Role",
                            "Failure",
                            empId,
                            null,
                            empId,
                            Map.of("error", "Current user not found", "roleId", roleId, "requestedStatus", isActive),
                            request
                    );
                    return "Current user not found";
                }
            }

            Optional<Employee> empOpt = employeeRepository.findById(empId);
            if (empOpt.isEmpty()) {
                log.info("FAILED → Change Role Status | employeeId={} reason=Employee not found", empId);

                auditLogUtil.logAction(
                        currentEmployee,
                        "ManageEmployeeRoles",
                        "Update Role",
                        "Failure",
                        empId,
                        null,
                        empId,
                        Map.of("error", "Employee not found with id " + empId, "roleId", roleId, "requestedStatus", isActive),
                        request
                );
                return "Employee not found.";
            }

            Employee employee = empOpt.get();

            Optional<EmployeeRole> employeeRoleOpt = employeeRoleRepository.findByEmpId_IdAndRoleId_Id(empId, roleId);

            if (employeeRoleOpt.isEmpty()) {
                log.info("FAILED → Change Role Status | employeeId={} roleId={} reason=Role not found for employee",
                        empId, roleId);

                auditLogUtil.logAction(
                        currentEmployee,
                        "ManageEmployeeRoles",
                        "Update Role",
                        "Failure",
                        empId,
                        employee.getName(),
                        empId,
                        Map.of("error", "Role not found for the employee", "employeeName", employee.getName(),
                                "roleId", roleId, "requestedStatus", isActive),
                        request
                );
                return "Role not found for the employee.";
            }

            EmployeeRole employeeRole = employeeRoleOpt.get();

            List<EmployeeRole> allExistingRoles = employeeRoleRepository.findByEmpId_Id(empId);
            List<String> existingActiveRoles = allExistingRoles.stream()
                    .filter(EmployeeRole::isActive)
                    .map(er -> er.getRoleId().getRole())
                    .toList();

            Map<String, Object> previousData = Map.of(
                    "name", employee.getName(),
                    "email", employee.getEmail(),
                    "previousRoles", existingActiveRoles,
                    "branch", employee.getBranch() != null ? employee.getBranch().getName() : null,
                    "department", employee.getDepartment() != null ? employee.getDepartment().getName() : null
            );

            employeeRole.setActive(isActive);
            employeeRole.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
            employeeRole.setUpdatedBy(currentEmployee);

            employeeRoleRepository.save(employeeRole);

            List<EmployeeRole> updatedRoles = employeeRoleRepository.findByEmpId_Id(empId);
            List<String> updatedActiveRoles = updatedRoles.stream()
                    .filter(EmployeeRole::isActive)
                    .map(er -> er.getRoleId().getRole())
                    .collect(Collectors.toList());

            Map<String, Object> finalAuditData = new HashMap<>(previousData);
            finalAuditData.put("newRoles", updatedActiveRoles);

            auditLogUtil.logAction(
                    currentEmployee,
                    "ManageEmployeeRoles",
                    "Update Role",
                    "Success",
                    empId,
                    employee.getName(),
                    empId,
                    finalAuditData,
                    request
            );

            log.info("SUCCESS → Role Status Changed | employeeId={} employeeName={} roleId={} newStatus={}",
                    empId, employee.getName(), roleId, isActive);

            return "Role status updated successfully.";

        } catch (Exception e) {
            log.error("FAILED → Change Role Status | employeeId={} roleId={} error={}",
                    empId, roleId, e.getMessage(), e);

            auditLogUtil.logAction(
                    updatedByEmployee,
                    "ManageEmployeeRoles",
                    "Update Role",
                    "Error",
                    empId,
                    null,
                    empId,
                    Map.of("exception", e.getMessage(), "roleId", roleId, "requestedStatus", isActive),
                    request
            );

            return "Failed to update role status due to an unexpected error";
        }
    }

    // ======================= GET EMPLOYEE WITH ROLES BY ID =======================
    public EmployeeRoleResponse getEmployeeWithRolesById(Integer employeeId) {

        log.info("API CALL → Get Employee With Roles | employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.info("FAILED → Get Employee With Roles | employeeId={} reason=Employee not found", employeeId);
                    return new RuntimeException("Employee with ID " + employeeId + " not found.");
                });

        List<EmployeeRole> employeeRoles = employeeRoleRepository.findAllRolesByEmployeeId(employeeId);
        List<String> roleNamesList = employeeRoles.stream()
                .map(role -> role.getRoleId() != null ? role.getRoleId().getRole() : "Unknown Role")
                .collect(Collectors.toList());

        log.info("SUCCESS → Retrieved Employee With Roles | employeeId={} name={} roles={}",
                employeeId, employee.getName(), roleNamesList);

        return new EmployeeRoleResponse(
                employee.getId(),
                employee.getName(),
                employee.getBranch() != null ? employee.getBranch().getName() : null,
                employee.getDepartment() != null ? employee.getDepartment().getName() : null,
                employee.getRole() != null ? employee.getRole().getRole() : null,
                roleNamesList,
                employee.isActive() ? "Active" : "Inactive",
                employee.getCreatedOn(),
                employee.getUpdatedOn(),
                employee.getCreatedBy() != null ? employee.getCreatedBy().getId() : null,
                employee.getCreatedBy() != null ? employee.getCreatedBy().getName() : null,
                employee.getUpdatedBy() != null ? employee.getUpdatedBy().getId() : null,
                employee.getUpdatedBy() != null ? employee.getUpdatedBy().getName() : null,
                employee.getMobile(),
                employee.getEmail()
        );
    }

    // ======================= GET EMPLOYEE WITH ACTIVE ROLES BY ID =======================
    @Override
    public EmployeeRoleResponse getEmployeeWithActiveRolesById(Integer empId) {

        log.info("API CALL → Get Employee With Active Roles | employeeId={}", empId);

        Employee employee = employeeRepository.findById(empId)
                .orElseThrow(() -> {
                    log.info("FAILED → Get Employee With Active Roles | employeeId={} reason=Employee not found", empId);
                    return new RuntimeException("Employee with ID " + empId + " not found.");
                });

        List<EmployeeRole> employeeRoles = employeeRoleRepository.findByEmpIdAndActive(empId);
        List<String> roleNamesList = employeeRoles.stream()
                .map(role -> role.getRoleId().getRole())
                .collect(Collectors.toList());

        log.info("SUCCESS → Retrieved Employee With Active Roles | employeeId={} name={} activeRoles={}",
                empId, employee.getName(), roleNamesList);

        return new EmployeeRoleResponse(
                employee.getId(),
                employee.getName(),
                employee.getBranch() != null ? employee.getBranch().getName() : null,
                employee.getDepartment() != null ? employee.getDepartment().getName() : null,
                employee.getRole() != null ? employee.getRole().getRole() : null,
                roleNamesList,
                employee.isActive() ? "Active" : "Inactive",
                employee.getCreatedOn(),
                employee.getUpdatedOn(),
                employee.getCreatedBy() != null ? employee.getCreatedBy().getId() : null,
                employee.getCreatedBy() != null ? employee.getCreatedBy().getName() : null,
                employee.getUpdatedBy() != null ? employee.getUpdatedBy().getId() : null,
                employee.getUpdatedBy() != null ? employee.getUpdatedBy().getName() : null,
                employee.getMobile(),
                employee.getEmail()
        );
    }

    // ======================= GET ALL EMPLOYEES WITH ROLES =======================
    @Override
    public List<AllEmployeeRoleResponse> getAllEmployeesWithRoles() {

        log.info("API CALL → Get All Employees With Roles");

        List<Employee> employees = employeeRepository.findByRoleIsNotNullOrdered();

        List<AllEmployeeRoleResponse> responses = employees.stream()
                .map(employee -> {
                    List<EmployeeRole> employeeRoles = employeeRoleRepository.findAllRolesByEmployeeId(employee.getId());

                    AllEmployeeRoleResponse response = new AllEmployeeRoleResponse(
                            employee.getId(),
                            employee.getName(),
                            employee.getBranch() != null ? employee.getBranch().getName() : null,
                            employee.getDepartment() != null ? employee.getDepartment().getName() : null,
                            employee.getRole() != null ? employee.getRole().getRole() : null,
                            null,
                            employee.isActive() ? "Active" : "Inactive",
                            employee.getCreatedOn(),
                            employee.getUpdatedOn(),
                            employee.getCreatedBy() != null ? employee.getCreatedBy().getId() : null,
                            employee.getCreatedBy() != null ? employee.getCreatedBy().getName() : null,
                            employee.getUpdatedBy() != null ? employee.getUpdatedBy().getId() : null,
                            employee.getUpdatedBy() != null ? employee.getUpdatedBy().getName() : null,
                            employee.getMobile(),
                            employee.getEmail()
                    );

                    response.setEmployeeRoles(employeeRoles);
                    return response;
                })
                .sorted(Comparator.comparing(AllEmployeeRoleResponse::getUpdatedOn).reversed())
                .collect(Collectors.toList());

        log.info("SUCCESS → Retrieved {} Employees With Roles", responses.size());

        return responses;
    }

    // ======================= GET EMPLOYEES BY BRANCH ID =======================
    @Override
    public List<AllEmployeeRoleResponse> getEmployeesByBranchId(Integer branchId) {

        log.info("API CALL → Get Employees By Branch | branchId={}", branchId);

        List<Employee> employees = employeeRepository.findByBranch_IdAndRoleNotNull(branchId);

        List<AllEmployeeRoleResponse> responses = employees.stream()
                .map(employee -> {
                    List<EmployeeRole> employeeRoles = employeeRoleRepository.findAllRolesByEmployeeId(employee.getId());

                    AllEmployeeRoleResponse response = new AllEmployeeRoleResponse(
                            employee.getId().intValue(),
                            employee.getName(),
                            employee.getBranch() != null ? employee.getBranch().getName() : null,
                            employee.getDepartment() != null ? employee.getDepartment().getName() : null,
                            employee.getRole() != null ? employee.getRole().getRole() : null,
                            null,
                            employee.isActive() ? "Active" : "Inactive",
                            employee.getCreatedOn(),
                            employee.getUpdatedOn(),
                            employee.getCreatedBy() != null ? employee.getCreatedBy().getId() : null,
                            employee.getCreatedBy() != null ? employee.getCreatedBy().getName() : null,
                            employee.getUpdatedBy() != null ? employee.getUpdatedBy().getId() : null,
                            employee.getUpdatedBy() != null ? employee.getUpdatedBy().getName() : null,
                            employee.getMobile(),
                            employee.getEmail()
                    );

                    response.setEmployeeRoles(employeeRoles);
                    return response;
                })
                .collect(Collectors.toList());

        log.info("SUCCESS → Retrieved {} Employees for Branch ID: {}", responses.size(), branchId);

        return responses;
    }
}