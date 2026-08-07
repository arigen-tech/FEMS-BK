package com.dmsBackend.service;

import com.dmsBackend.entity.Employee;
import com.dmsBackend.entity.EmployeeRole;
import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.response.AllEmployeeRoleResponse;
import com.dmsBackend.response.EmployeeResponse;
import com.dmsBackend.response.EmployeeRoleResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public interface EmployeeRoleService {

    public EmployeeRole updateEmployeeRole(Employee identifier, RoleMaster roleName, Employee currentUser);


   // public String deactivateEmployeeRole(Integer empId, Integer roleId);


    @Transactional
//    EmployeeRole createOrUpdateEmployeeRole(Integer empId, Integer roleId);
    public EmployeeRole createOrUpdateEmployeeRole(Integer empId, Integer roleId, HttpServletRequest request);
    List<EmployeeRole> findAll();

    EmployeeRoleResponse getEmployeeWithRolesById(Integer employeeId);

   // String activateEmployeeRole(Integer empId, Integer roleId);

//    public String changeRoleStatus(Integer empId, Integer roleId, boolean isActive, Employee updatedByEmployee);

    public String changeRoleStatus(Integer empId, Integer roleId, boolean isActive, Employee updatedByEmployee, HttpServletRequest request);
    EmployeeRoleResponse getEmployeeWithActiveRolesById(Integer empId);

    public List<AllEmployeeRoleResponse> getAllEmployeesWithRoles() ;


    //List<AllEmployeeRoleResponse> getEmployeesByBranch(String branchName);

    List<AllEmployeeRoleResponse> getEmployeesByBranchId(Integer branchId);
}
