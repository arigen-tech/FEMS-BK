package com.dmsBackend.ArchiveCodes;

import com.dmsBackend.entity.RetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArchiveJobRepository extends JpaRepository<ArchiveJob, Long> {
    Optional<ArchiveJob> findByRetentionPolicy(RetentionPolicy retentionPolicy);

    ArchiveJob findByRetentionPolicyId (Long id);

    @Query("""
        SELECT aj FROM ArchiveJob aj
        WHERE (:branchId IS NULL OR aj.branch.id = :branchId)
          AND (:deptId IS NULL OR aj.department.id = :deptId)
          AND (:status IS NULL OR aj.status = :status)
        ORDER BY aj.createdOn DESC
    """)
    List<ArchiveJob> findByFilters(
            @Param("branchId") Integer branchId,
            @Param("deptId") Integer deptId,
            @Param("status") ArchiveJob.Status status
    );

    List<ArchiveJob> findByStatus(ArchiveJob.Status inProgress);
}
