package com.dmsBackend.repository;

import com.dmsBackend.entity.*;
import com.dmsBackend.response.StatusCountByYearDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    // Find an employee by email
    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    Optional<Employee> findByRoleAndBranch(RoleMaster role, BranchMaster branch);

    // ✅ Portable query for branch employees with ordering
    @Query(value = """
        SELECT *
        FROM employee e
        WHERE e.department_master_branch_master_id = :#{#branch.id}
        ORDER BY
            CASE
                WHEN e.is_active = TRUE AND DATE(e.updated_on) = CURRENT_DATE THEN 0
                WHEN e.is_active = TRUE THEN 1
                ELSE 2
            END ASC,
            e.updated_on DESC
        """, nativeQuery = true)
    List<Employee> findByBranchOrdered(@Param("branch") BranchMaster branch);

    List<Employee> findEmployeesByCreatedBy(Employee employee);

    // ✅ Employees with null role
    @Query(value = """
        SELECT *
        FROM employee e
        WHERE e.role_id IS NULL
        ORDER BY
            CASE
                WHEN e.is_active = TRUE AND DATE(e.updated_on) = CURRENT_DATE THEN 0
                WHEN e.is_active = TRUE THEN 1
                ELSE 2
            END ASC,
            e.updated_on DESC
        """, nativeQuery = true)
    List<Employee> findByRoleIsNullOrdered();

    List<Employee> findByIdAndRoleIsNull(Integer id);

    List<Employee> findAllByRoleIsNotNull();

    @Query(value = """
    SELECT *
    FROM employee e
    WHERE e.role_id IS NOT NULL
            ORDER BY
        CASE
            WHEN e.is_active = TRUE AND DATE(e.updated_on) = CURRENT_DATE THEN 0
            WHEN e.is_active = TRUE THEN 1
            ELSE 2
        END ASC,
        e.updated_on DESC
    """, nativeQuery = true)
    List<Employee> findByRoleIsNotNullOrdered();


    List<Employee> findAllByRoleIsNotNullAndBranch(BranchMaster branchId);

    List<Employee> findAllByRoleIsNotNullAndDepartment(DepartmentMaster department);

    long countByRoleIsNull();

    long countByRoleIsNotNull();

    long countByRole(RoleMaster roleMaster);

    List<Employee> findByRole(RoleMaster roleMaster);

    Optional<Employee> findByBranchAndRole(BranchMaster branch, RoleMaster role);

    long countByBranchId(Integer branchId);

    // ✅ Null role + branch filter
    @Query(value = """
        SELECT *
        FROM employee e
        WHERE e.role_id IS NULL
          AND e.department_master_branch_master_id = :#{#branch.id}
        ORDER BY
            CASE
                WHEN e.is_active = TRUE AND DATE(e.updated_on) = CURRENT_DATE THEN 0
                WHEN e.is_active = TRUE THEN 1
                ELSE 2
            END ASC,
            e.updated_on DESC
        """, nativeQuery = true)
    List<Employee> findByRoleIsNullAndBranchOrdered(@Param("branch") BranchMaster branch);

    long countByBranchIdAndRoleIsNull(Integer branchId);

    Optional<Employee> findByRoleAndDepartment(RoleMaster newRole, DepartmentMaster department);

    // ✅ Department filter
    @Query(value = """
        SELECT *
        FROM employee e
        WHERE e.department_master_id = :#{#department.id}
        ORDER BY
            CASE
                WHEN e.is_active = TRUE AND DATE(e.updated_on) = CURRENT_DATE THEN 0
                WHEN e.is_active = TRUE THEN 1
                ELSE 2
            END ASC,
            e.updated_on DESC
        """, nativeQuery = true)
    List<Employee> findByDepartmentOrdered(@Param("department") DepartmentMaster department);

    // ✅ Null role + department filter
    @Query(value = """
        SELECT *
        FROM employee e
        WHERE e.role_id IS NULL
          AND e.department_master_id = :#{#department.id}
        ORDER BY
            CASE
                WHEN e.is_active = TRUE AND DATE(e.updated_on) = CURRENT_DATE THEN 0
                WHEN e.is_active = TRUE THEN 1
                ELSE 2
            END ASC,
            e.updated_on DESC
        """, nativeQuery = true)
    List<Employee> findByRoleIsNullAndDepartmentOrdered(@Param("department") DepartmentMaster department);

    long countByDepartmentId(Integer departmentId);

    long countByDepartmentIdAndRoleIsNull(Integer departmentId);

    long countByCreatedById(Integer createdById);

    @Query("""
        SELECT e FROM Employee e
        WHERE e.branch.id = :departmentMasterBranchId
        AND e.department.id = :departmentMasterId
        AND (:status IS NULL OR e.isActive = :status)
        AND e.createdOn BETWEEN :startDate AND :endDate
        """)
    List<Employee> findByFilters(
            @Param("departmentMasterBranchId") Integer departmentMasterBranchId,
            @Param("departmentMasterId") Integer departmentMasterId,
            @Param("status") Boolean status,
            @Param("startDate") Timestamp startDate,
            @Param("endDate") Timestamp endDate
    );

    @Query("SELECT e FROM Employee e WHERE e.role = :role")
    Optional<Employee> findSingleByRole(@Param("role") RoleMaster role);

    List<Employee> findByBranch_IdAndRoleNotNull(Integer branchId);

    Optional<Employee> findById(Integer empId);

    @Query("SELECT er.empId FROM EmployeeRole er " +
            "WHERE er.roleId.role = :roleName " +
            "AND er.empId.department.id = :departmentId " +
            "AND er.isActive = true")
    List<Employee> findByDepartmentIdAndRoleRole(
            @Param("departmentId") Integer departmentId,
            @Param("roleName") String roleName
    );

    Optional<Employee> findByMobile(String mobile);

    // ✅ Works in both MySQL + PostgreSQL
    @Query("""
        SELECT new com.dmsBackend.response.StatusCountByYearDto(
            FUNCTION('YEAR', u.createdOn),
            SUM(CASE WHEN u.isActive THEN 1 ELSE 0 END),
            SUM(CASE WHEN u.isActive = false THEN 1 ELSE 0 END),
            SUM(CASE WHEN u.role IS NULL THEN 1 ELSE 0 END)
        )
        FROM Employee u
        GROUP BY FUNCTION('YEAR', u.createdOn)
        ORDER BY FUNCTION('YEAR', u.createdOn)
        """)
    List<StatusCountByYearDto> getStatusCountGroupedByYear();

    // ✅ General ordering (portable)
    @Query(value = """
        SELECT *
        FROM employee e
        ORDER BY
            CASE
                WHEN e.is_active = TRUE AND DATE(e.updated_on) = CURRENT_DATE THEN 0
                WHEN e.is_active = TRUE THEN 1
                ELSE 2
            END ASC,
            e.updated_on DESC
        """, nativeQuery = true)
    List<Employee> findEmployeesOrdered();

    // In EmployeeRepository.java
    @Query("SELECT e FROM Employee e WHERE e.department.id = :departmentId")
    List<Employee> findByDepartmentId(@Param("departmentId") Integer departmentId);

    @Query("""
        SELECT e 
        FROM Employee e
        WHERE e.branch.id = :branchId
          AND e.department.id = :departmentId
    """)
    List<Employee> findByBranchAndDepartment(
            @Param("branchId") Integer branchId,
            @Param("departmentId") Integer departmentId
    );
}
