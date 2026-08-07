package com.dmsBackend.repository;

import com.dmsBackend.entity.RetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicy, Long> {

    @Query("SELECT r FROM RetentionPolicy r WHERE r.isActive = true")
    List<RetentionPolicy> findAllActive();

    List<RetentionPolicy> findByIsActiveTrue();

    @Query("SELECT rp FROM RetentionPolicy rp WHERE rp.department.id = :departmentId AND rp.isActive = true")
    List<RetentionPolicy> findByDepartmentIdAndActive(@Param("departmentId") Integer departmentId);

    @Query("SELECT rp FROM RetentionPolicy rp WHERE rp.category.id = :categoryId AND rp.isActive = true")
    List<RetentionPolicy> findByCategoryIdAndActive(@Param("categoryId") Integer categoryId);

    @Query("SELECT rp FROM RetentionPolicy rp WHERE rp.department.id = :departmentId AND rp.category.id = :categoryId AND rp.isActive = true")
    Optional<RetentionPolicy> findByDepartmentIdAndCategoryIdAndActive(
            @Param("departmentId") Integer departmentId,
            @Param("categoryId") Integer categoryId);

    @Query("SELECT p FROM RetentionPolicy p WHERE p.branch.id = :branchId AND (p.department.id = :departmentId OR p.department.id IS NULL)")
    List<RetentionPolicy> findByBranchIdAndDepartmentIdOrAll(
            @Param("branchId") Integer branchId,
            @Param("departmentId") Integer departmentId
    );


    List<RetentionPolicy> findByBranchIdAndIsActiveTrue(Integer branchId);

//    @Query("SELECT r FROM RetentionPolicy r " +
//            "WHERE (:excludeId IS NULL OR r.id <> :excludeId) " +
//            "AND r.fromdate <= :toDate " +
//            "AND r.todate >= :fromDate")
//    List<RetentionPolicy> findOverlappingPolicies(
//            @Param("fromDate") LocalDateTime fromDate,
//            @Param("toDate") LocalDateTime toDate,
//            @Param("excludeId") Long excludeId
//    );

    @Query("""
    SELECT r FROM RetentionPolicy r
    WHERE (:excludeId IS NULL OR r.id <> :excludeId)
      AND r.policyType = :policyType
      AND r.fromDate <= :toDate
      AND r.toDate   >= :fromDate
      AND (:reqBranchId IS NULL OR r.branch.id = :reqBranchId)
      AND (:reqDeptId  IS NULL OR r.department.id = :reqDeptId)
      AND (:reqCatId   IS NULL OR r.category.id = :reqCatId)
""")
    List<RetentionPolicy> findOverlappingPolicies(
            @Param("policyType") RetentionPolicy.PolicyType policyType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("reqBranchId") Long reqBranchId,
            @Param("reqDeptId") Long reqDeptId,
            @Param("reqCatId") Long reqCatId,
            @Param("excludeId") Long excludeId
    );



    @Query(value = """
    SELECT *
    FROM retention_policy r
    ORDER BY
        CASE WHEN r.is_active = true THEN 0 ELSE 1 END,
        ABS(EXTRACT(EPOCH FROM (make_timestamp(
            EXTRACT(YEAR FROM r.retention_date),
            EXTRACT(MONTH FROM r.retention_date),
            EXTRACT(DAY FROM r.retention_date),
            EXTRACT(HOUR FROM r.retention_time),
            EXTRACT(MINUTE FROM r.retention_time),
            EXTRACT(SECOND FROM r.retention_time)
        ) - CURRENT_TIMESTAMP)))
    """, nativeQuery = true)
    List<RetentionPolicy> findAllOrderByRetention();


    @Query("""
        select rp
        from RetentionPolicy rp
        where rp.isActive = true
          and (
                (rp.retentionDate is null and rp.retentionTime is null)
                or
                (function('timestamp', rp.retentionDate, rp.retentionTime) <= :now)
              )
    """)
    List<RetentionPolicy> findActiveAndDue(@Param("now") LocalDateTime now);


    Optional<RetentionPolicy>
    findByFromDateLessThanEqualAndToDateGreaterThanEqual(
            LocalDateTime from,
            LocalDateTime to
    );

    List<RetentionPolicy> findByArchiveStatus(String in_progress);

    Long countBy();

    Long countByArchiveStatus(String archiveStatus);

    @Query("""
    SELECT r
    FROM RetentionPolicy r
    WHERE :approvedOn BETWEEN r.fromDate AND r.toDate
    
""")
    RetentionPolicy findActivePolicyByApprovedOn(
            @Param("approvedOn") LocalDateTime approvedOn
    );


    @Query("""
    SELECT r FROM RetentionPolicy r
    LEFT JOIN FETCH r.branch
    LEFT JOIN FETCH r.department
    WHERE (:branchId IS NULL OR r.branch.id = :branchId)
      AND (:departmentId IS NULL OR r.department.id = :departmentId)
""")
    List<RetentionPolicy> findByBranchAndDepartment(
            @Param("branchId") Long branchId,
            @Param("departmentId") Long departmentId
    );
}
