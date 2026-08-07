package com.dmsBackend.repository;

import com.dmsBackend.entity.CategoryMaster;
import com.dmsBackend.entity.YearMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface YearMasterRepository extends JpaRepository<YearMaster,Integer> {

    List<YearMaster> findByIsActive(int isActive);
    boolean existsByName(String name);


    Optional<YearMaster> findByName(String yearName);

    @Query(value = """
        SELECT *
        FROM year_master y
        ORDER BY
            CASE
                WHEN y.is_active = 1 AND DATE(y.updated_on) = CURRENT_DATE THEN 0
                WHEN y.is_active = 1 THEN 1
                ELSE 2
            END ASC,
            y.updated_on DESC
        """, nativeQuery = true)
    List<YearMaster> findAllYearMasterOrdered();




}
