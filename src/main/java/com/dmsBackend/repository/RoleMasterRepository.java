package com.dmsBackend.repository;

import com.dmsBackend.entity.RoleMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleMasterRepository extends JpaRepository<RoleMaster, Integer> {

    Optional<RoleMaster> findByRole(String role);

    List<RoleMaster> findByIsActive(boolean isActive);



    @Query(value = """
        SELECT *
        FROM role_master r
        ORDER BY
            CASE
                WHEN r.is_active = TRUE AND DATE(r.updated_on) = CURRENT_DATE THEN 0
                WHEN r.is_active = TRUE THEN 1
                ELSE 2
            END ASC,
            r.updated_on DESC
        """, nativeQuery = true)
    List<RoleMaster> findAllRoleMasterOrdered();



}
