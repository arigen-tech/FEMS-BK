package com.dmsBackend.ArchiveCodes;


import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.entity.DepartmentMaster;
import com.dmsBackend.entity.RetentionPolicy;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "archive_jobs",
        indexes = {
                @Index(name = "idx_archive_job_policy_type", columnList = "policy_type"),
                @Index(name = "idx_archive_job_archive_name", columnList = "archive_name"),
                @Index(name = "idx_archive_job_from_date", columnList = "from_date"),
                @Index(name = "idx_archive_job_to_date", columnList = "to_date"),
                @Index(name = "idx_archive_job_archive_datetime", columnList = "archive_datetime"),
                @Index(name = "idx_archive_job_status", columnList = "status"),
                @Index(name = "idx_archive_job_created_on", columnList = "created_on"),
                @Index(name = "idx_archive_job_archived_on", columnList = "archived_on"),
                @Index(name = "idx_archive_job_retention_policy", columnList = "retention_policy_id"),
                @Index(name = "idx_archive_job_branch", columnList = "branch_id"),
                @Index(name = "idx_archive_job_department", columnList = "department_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArchiveJob {

    // ---------- Primary Key ----------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------- Relations ----------
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retention_policy_id", nullable = false)
    private RetentionPolicy retentionPolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private BranchMaster branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentMaster department;

    // ---------- Core Attributes ----------
    @Column(name = "policy_type", nullable = false, length = 50)
    private String policyType;

    @Column(name = "archive_name", nullable = false, length = 255)
    private String archiveName;

    @Column(name = "description", length = 1000)
    private String description;

    // ---------- Date & Time Fields ----------
    @Column(name = "from_date", nullable = false)
    private LocalDateTime fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDateTime toDate;

    @Column(name = "archive_datetime", nullable = false)
    private LocalDateTime archiveDateTime;

    @Column(name = "archived_on")
    private LocalDateTime archivedOn;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    // ---------- Status ----------
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status = Status.WAITING;

    // ---------- Document Counters (DocumentHeader)----------
    @Column(name = "total_documents", nullable = false)
    private Integer totalDocuments = 0;

    @Column(name = "archived_documents", nullable = false)
    private Integer archivedDocuments = 0;

    @Column(name = "failed_documents", nullable = false)
    private Integer failedDocuments = 0;

    // ---------- File Counters (DocumentDetails)----------
    @Column(name = "total_files", nullable = false)
    private Integer totalFiles = 0;

    @Column(name = "archived_files", nullable = false)
    private Integer archivedFiles = 0;

    @Column(name = "failed_files", nullable = false)
    private Integer failedFiles = 0;

    // ---------- Entity Lifecycle ----------
    @PrePersist
    protected void onCreate() {
        if (createdOn == null) {
            createdOn = LocalDateTime.now();
        }
    }

    // ---------- Enum ----------
    public enum Status {
        WAITING,
        IN_PROGRESS,
        ARCHIVED,
        FAILED,
        PARTIAL_SUCCESS
    }
}