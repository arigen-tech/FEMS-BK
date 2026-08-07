package com.dmsBackend.controller;

import com.dmsBackend.entity.Employee;
import com.dmsBackend.entity.EmployeeRole;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.response.*;
import com.dmsBackend.response.ApiResponse;
import com.dmsBackend.service.EmployeeRoleService;
import com.dmsBackend.service.EmployeeService;
import com.dmsBackend.utils.ResponseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/EmpRole")
@Slf4j
public class EmployeeRoleController {

    @Autowired
    private EmployeeRoleService employeeRoleService;

    @Autowired
    private EmployeeService employeeService;

    @PutMapping("/changeRoleStatus")
    public ResponseEntity<ApiResponse<String>> changeRoleStatus(
            @RequestBody ChangeRoleStatusRequest request,
            HttpServletRequest servletRequest) {

        log.info("ChangeRoleStatus request received | EmpId={} | RoleId={} | Status={}",
                request.getEmpId(), request.getRoleId(), request.isStatus());

        User currentUser =
                (User) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        Employee currentEmployee =
                employeeService.findByEmail(currentUser.getUsername());

        String responseMessage =
                employeeRoleService.changeRoleStatus(
                        request.getEmpId(),
                        request.getRoleId(),
                        request.isStatus(),
                        currentEmployee,
                        servletRequest
                );

        if ("Role status updated successfully.".equals(responseMessage)) {
            log.info("Role status updated successfully | EmpId={} | RoleId={}",
                    request.getEmpId(), request.getRoleId());

            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            responseMessage,
                            new TypeReference<>() {}
                    )
            );
        } else {
            log.warn("Role status update failed | EmpId={} | RoleId={} | Reason={}",
                    request.getEmpId(), request.getRoleId(), responseMessage);

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ResponseUtils.createNotFoundResponse(
                                    responseMessage,
                                    HttpStatus.NOT_FOUND.value()
                            )
                    );
        }
    }

    @PostMapping("/assign")
    public ResponseEntity<EmployeeRole> assignRole(
            @RequestParam Integer empId,
            @RequestParam Integer roleId,
            HttpServletRequest request) {

        log.info("Assign role request | EmpId={} | RoleId={}", empId, roleId);

        EmployeeRole result =
                employeeRoleService.createOrUpdateEmployeeRole(empId, roleId, request);

        log.info("Role assigned successfully | EmpId={} | RoleId={}", empId, roleId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/employee/{employeeId}")
    public EmployeeRoleResponse getEmployeeWithRoles(@PathVariable Integer employeeId) {

        log.info("Fetching employee roles | EmployeeId={}", employeeId);
        return employeeRoleService.getEmployeeWithRolesById(employeeId);
    }

    @GetMapping("/employees")
    public ResponseEntity<List<AllEmployeeRoleResponse>> getAllEmployeesWithRoles() {

        log.info("Fetching all employees with roles");
        List<AllEmployeeRoleResponse> response =
                employeeRoleService.getAllEmployeesWithRoles();

        log.info("Total employees fetched: {}", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{empId}/roles/active")
    public EmployeeRoleResponse getEmployeeWithActiveRoles(@PathVariable Integer empId) {

        log.info("Fetching active roles | EmployeeId={}", empId);
        return employeeRoleService.getEmployeeWithActiveRolesById(empId);
    }

    @GetMapping("/getAll")
    public List<EmployeeRole> getAll() {

        log.info("Fetching all EmployeeRole records");
        return employeeRoleService.findAll();
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<AllEmployeeRoleResponse>> getEmployeesByBranchId(
            @PathVariable Integer branchId) {

        log.info("Fetching employees by BranchId={}", branchId);

        List<AllEmployeeRoleResponse> response =
                employeeRoleService.getEmployeesByBranchId(branchId);

        log.info("Employees found for BranchId={}: {}", branchId, response.size());
        return ResponseEntity.ok(response);
    }
}
