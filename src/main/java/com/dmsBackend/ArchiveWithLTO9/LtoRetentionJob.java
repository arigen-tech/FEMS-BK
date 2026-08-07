package com.dmsBackend.ArchiveWithLTO9;

import com.dmsBackend.entity.RetentionPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "lto_retention_jobs")
@Getter
@Setter
@ToString
public class LtoRetentionJob {

    public enum JobStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CLEANED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Policy reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retention_policy_id", nullable = false)
    private RetentionPolicy retentionPolicy;

    // 🔥 IMPORTANT FIX
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private JobStatus status;

    @Column(name = "started_on")
    private LocalDateTime startedOn;

    @Column(name = "completed_on")
    private LocalDateTime completedOn;

    @Column(name = "total_files")
    private Integer totalFiles = 0;

    @Column(name = "archived_files")
    private Integer archivedFiles = 0;

    @Column(name = "failed_files")
    private Integer failedFiles = 0;

    @Column(name = "total_headers")
    private Integer totalHeaders = 0;

    @Column(name = "archived_headers")
    private Integer archivedHeaders = 0;

    @Column(name = "failed_headers")
    private Integer failedHeaders = 0;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

}
