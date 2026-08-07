package com.dmsBackend.ArchiveWithLTO9;

import com.dmsBackend.entity.RetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LtoRetentionJobRepository
        extends JpaRepository<LtoRetentionJob, Long> {

    Optional<LtoRetentionJob> findByRetentionPolicy(RetentionPolicy policy);

    Optional<LtoRetentionJob>
    findByRetentionPolicyAndStatusIn(
            RetentionPolicy policy,
            List<LtoRetentionJob.JobStatus> statuses
    );

    @Modifying
    @Query("update LtoRetentionJob j set j.totalHeaders = :total where j.id = :jobId")
    void updateTotalHeaders(Long jobId, Integer total);

    @Modifying
    @Query("update LtoRetentionJob j set j.totalFiles = :total where j.id = :jobId")
    void updateTotalFiles(Long jobId, Integer total);




    @Query("""
    select j from LtoRetentionJob j
    join fetch j.retentionPolicy
    where j.id = :jobId
""")
    Optional<LtoRetentionJob> findByIdWithPolicy(Long jobId);


    @Query("""
        select j from LtoRetentionJob j
        join fetch j.retentionPolicy
        where j.status = 'PENDING'
    """)
    List<LtoRetentionJob> findPendingJobs();

    @Query("""
    select j from LtoRetentionJob j
    join fetch j.retentionPolicy p
    where j.status = 'COMPLETED'
      and p.isActive = false
      and p.toDate < :cutoffDate
""")
    List<LtoRetentionJob> findEligibleForCleanup(LocalDate cutoffDate);


    Optional<LtoRetentionJob> findByRetentionPolicyId(Long retentionPolicyId);
}
