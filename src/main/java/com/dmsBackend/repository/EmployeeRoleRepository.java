package com.dmsBackend.repository;

import com.dmsBackend.entity.Employee;
import com.dmsBackend.entity.EmployeeRole;
import com.dmsBackend.entity.RoleMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, Integer> {
    Optional<EmployeeRole> findByEmpId(Employee employee);

    List<EmployeeRole> findAllByEmpId(Employee employee);

    Optional<EmployeeRole> findByEmpId_IdAndRoleId_Id(Integer empId, Integer roleId);


    @Query("SELECT er FROM EmployeeRole er WHERE er.empId.id = :employeeId")
    List<EmployeeRole> findAllRolesByEmployeeId(Integer employeeId);

    @Query("SELECT er FROM EmployeeRole er WHERE er.empId.id = :empId AND er.isActive = true")
    List<EmployeeRole>findByEmpIdAndActive(@Param("empId") Integer empId);


    List<EmployeeRole> findByEmpId_Id(Integer empId);

    @Query("SELECT er.empId FROM EmployeeRole er WHERE er.roleId.role = :roleName AND er.isActive = true AND er.empId.department.id = :departmentId AND er.empId.isActive = true")
    List<Employee> findActiveEmployeesByRoleNameAndDepartmentId(
            @Param("roleName") String roleName,
            @Param("departmentId") Integer departmentId);}
