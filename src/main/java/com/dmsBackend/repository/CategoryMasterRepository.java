package com.dmsBackend.repository;

import com.dmsBackend.entity.CategoryMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryMasterRepository extends JpaRepository<CategoryMaster,Integer> {
    List<CategoryMaster> findByActive(boolean active);
    List<CategoryMaster> findAllById(Integer id);
    @Query("SELECT c.name FROM CategoryMaster c WHERE c.id = :id")
    Optional<String> findNameById(@Param("id") Integer id);

    Optional<CategoryMaster> findByName(String categoryName);

    @Query(value = """
        SELECT *
        FROM category_master c
        ORDER BY
            CASE
                WHEN c.is_active = TRUE AND DATE(c.updated_on) = CURRENT_DATE THEN 0
                WHEN c.is_active = TRUE THEN 1
                ELSE 2
            END ASC,
            c.updated_on DESC
        """, nativeQuery = true)
    List<CategoryMaster> findAllCategoryMasterOrdered();



}
