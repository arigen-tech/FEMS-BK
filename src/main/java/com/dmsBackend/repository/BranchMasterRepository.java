package com.dmsBackend.repository;

import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.entity.RoleMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchMasterRepository extends JpaRepository<BranchMaster,Integer> {

    List<BranchMaster> findByIsActive(Integer isActive);

    @Query("SELECT b.name FROM BranchMaster b WHERE b.id = :id")
    Optional<String> findNameById(@Param("id") Integer id);

    Optional<BranchMaster> findByNameIgnoreCase(String branchName);


    @Query(value = """
    SELECT *
    FROM branch_master b
    ORDER BY
        CASE
            WHEN b.is_active = 1 AND CAST(b.updated_on AS DATE) = CURRENT_DATE THEN 0
            WHEN b.is_active = 1 THEN 1
            ELSE 2
        END ASC,
        b.updated_on DESC
    """, nativeQuery = true)
    List<BranchMaster> findAllBranchMasterOrdered();




}
