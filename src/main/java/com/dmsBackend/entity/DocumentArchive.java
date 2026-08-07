package com.dmsBackend.entity;

import com.dmsBackend.ArchiveCodes.ArchiveJob;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "document_archive",
        indexes = {
                @Index(name = "idx_docarchive_doc_id", columnList = "doc_id"),
                @Index(name = "idx_docarchive_archive_job_id", columnList = "archiveJobId"),
//                @Index(name = "idx_docarchive_comments", columnList = "comments"),
                @Index(name = "idx_docarchive_version", columnList = "version"),
                @Index(name = "idx_docarchive_status", columnList = "status"),
                @Index(name = "idx_docarchive_request_id", columnList = "requestid")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentArchive {

    // ───── Primary Key ─────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "archiveid")
    private Long archiveId;

    // ───── Relations ─────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id")
    private DocumentHeader documentHeader;   // FK relation

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archiveJobId", nullable = false)
    private ArchiveJob archiveJob;   // FK relation

    // ───── Core Document Info ─────
    @Column(name = "collectionname", length = 255)
    private String collectionName;

    @Column(name = "version", length = 10)
    private String version;

    @Column(name = "objectname", length = 255)
    private String objectName;

    @Column(name = "filepathroot", length = 500)
    private String filePathRoot;

    @Column(name = "media", length = 100)
    private String media;

    @Column(name = "sourceserver", length = 100)
    private String sourceServer;

    @Column(name = "priority")
    private Integer priority;

    // ───── JSON Fields ─────
    @Column(name = "components", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode components;

    @Column(name = "request_json", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode requestJson;

    // ───── Archive Status Info ─────
    @Column(name = "requestid", length = 200)
    private String requestId;

    @Column(name = "status", length = 50)
    private String status;   // Archived / Restored / Failed

    @Lob
    @Column(name = "archive_failure_reason", columnDefinition = "LONGTEXT")
    private String failedReason;


    // ───── Audit Fields ─────
    @Lob
    @Column(name = "comments", columnDefinition = "LONGTEXT")
    private String comments;

    @Column(name = "createdat", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}


