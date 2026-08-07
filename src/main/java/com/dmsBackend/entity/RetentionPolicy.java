package com.dmsBackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "retention_policies",
        indexes = {
                @Index(name = "idx_retention_archive_name", columnList = "archive_name"),
                @Index(name = "idx_retention_is_active", columnList = "is_active"),
                @Index(name = "idx_retention_policy_type", columnList = "policy_type"),
                @Index(name = "idx_retention_from_dt", columnList = "archive_from_dt"),
                @Index(name = "idx_retention_to_dt", columnList = "archive_to_dt"),
                @Index(name = "idx_retention_date", columnList = "retention_date"),
                @Index(name = "idx_retention_time", columnList = "retention_time"),
                @Index(name = "idx_retention_created_on", columnList = "created_on"),
                @Index(name = "idx_retention_updated_on", columnList = "updated_on"),
                @Index(name = "idx_retention_department", columnList = "department_id"),
                @Index(name = "idx_retention_branch", columnList = "branch_id"),
                @Index(name = "idx_retention_category", columnList = "category_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RetentionPolicy {

    // ---------- Primary Key ----------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------- Main Attributes ----------
    @Column(name = "archive_name", nullable = false, length = 255)
    private String archiveName;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 50)
    private PolicyType policyType = PolicyType.FILE_RETENTION;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "archive_status", length = 1000)
    private String archiveStatus;

    // ---------- Retention Details ----------
    @Column(name = "archive_from_dt")
    private LocalDateTime fromDate;

    @Column(name = "archive_to_dt")
    private LocalDateTime toDate;

    @Column(name = "retention_date")
    private LocalDate retentionDate;

    @Column(name = "retention_time")
    private LocalTime retentionTime;

    // ---------- Relations ----------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentMaster department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private BranchMaster branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryMaster category;

    // ---------- Audit Fields ----------
    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "updated_by")
    private Integer updatedBy;


    // ---------- Enum ----------
    public enum PolicyType {
        FILE_RETENTION,
        DATA_RETENTION
    }

    // ---------- Entity Lifecycle ----------
    @PrePersist
    protected void onCreate() {
        createdOn = LocalDateTime.now();
        updatedOn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedOn = LocalDateTime.now();
    }

    // ---------- Utility ----------
    public LocalDateTime getRetentionDateTime() {
        if (retentionDate == null || retentionTime == null) {
            return null;
        }
        return LocalDateTime.of(retentionDate, retentionTime);
    }
}
