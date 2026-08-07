package com.dmsBackend.repository;

import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.entity.DepartmentMaster;
import com.dmsBackend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DepartmentMasterRepository extends JpaRepository<DepartmentMaster,Integer> {

    List<DepartmentMaster> findByIsActive(Integer isActive);

    List<DepartmentMaster> findByBranchId(Integer branchId);

    long countByBranchId(Integer branchId); // Counts departments per branch

    @Query("SELECT d.name FROM DepartmentMaster d WHERE d.id = :id")
    Optional<String> findNameById(@Param("id") Integer id);

    Optional<DepartmentMaster> findByNameAndBranch(String deptName, BranchMaster branch);

    @Query(value = """
    SELECT *
    FROM department_master d
    ORDER BY
        CASE
            WHEN d.is_active = 1 AND CAST(d.updated_on AS DATE) = CURRENT_DATE THEN 0
            WHEN d.is_active = 1 THEN 1
            ELSE 2
        END ASC,
        d.updated_on DESC
    """, nativeQuery = true)
    List<DepartmentMaster> findAllDepartmentMasterOrdered();



}
