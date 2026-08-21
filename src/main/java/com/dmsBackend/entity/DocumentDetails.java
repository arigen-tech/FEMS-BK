package com.dmsBackend.entity;

import com.dmsBackend.ArchiveCodes.ArchiveJob;
import com.dmsBackend.ArchiveWithLTO9.LtoRetentionJob;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(
        name = "document_details",
        indexes = {
                @Index(name = "idx_doc_id", columnList = "id"),
                @Index(name = "idx_doc_file_name", columnList = "file_name"),
                @Index(name = "idx_doc_file_path", columnList = "file_path"),
                @Index(name = "idx_doc_version", columnList = "version"),
                @Index(name = "idx_doc_status", columnList = "status"),
                @Index(name = "idx_doc_header_id", columnList = "document_header_id"),
                @Index(name = "idx_doc_year_id", columnList = "year_id"),
                @Index(name = "idx_doc_archive_job_id", columnList = "archive_job_policy_id"),
                @Index(name = "idx_doc_arc_id", columnList = "DocArchive_id"),
                @Index(name = "idx_doc_archived", columnList = "archived"),
                @Index(name = "idx_doc_request_id", columnList = "archive_requestId"),
                @Index(name = "idx_doc_archival_status", columnList = "archival_status"),
//                @Index(name = "idx_doc_failure_reason", columnList = "archive_failure_reason"),
                @Index(name = "idx_doc_approved_on", columnList = "approved_on"),
                @Index(name = "idx_doc_approved_by", columnList = "approved_by"),
                @Index(name = "idx_doc_rejection_reason", columnList = "rejection_reason"),
                @Index(name = "idx_doc_created_on", columnList = "created_on"),
                @Index(name = "idx_doc_created_by", columnList = "created_by"),
                @Index(name = "idx_doc_updated_on", columnList = "updated_on"),
                @Index(name = "idx_doc_updated_by", columnList = "updated_by")
        }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DocumentDetails {

    // ───── Primary Key ─────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ───── Core Document Info ─────
    @Column(name = "file_name", nullable = false)
    private String docName;

    @Column(name = "file_path", nullable = false)
    private String path;

    @Column(name = "version")
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocApprovalStatus status = DocApprovalStatus.PENDING;

    // ───── Relations ─────
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_header_id", nullable = false)
    @JsonBackReference
    private DocumentHeader documentHeader;

    @ManyToOne
    @JoinColumn(name = "year_id", referencedColumnName = "id", nullable = false)
    private YearMaster yearMaster;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "archive_job_policy_id")
    private ArchiveJob archiveJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DocArchive_id")
    @JsonIgnore
    private DocumentArchive documentArchive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waiting_room_id")
    @JsonIgnore
    private WaitingRoom waitingRoomId;

    // ───── Duplicate / Delete Flags ─────
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted   = false;

    @Column(name = "is_duplicate", nullable = false)
    private Boolean isDuplicate = false;

    // ───── Self Reference (Original Document) ─────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", referencedColumnName = "id", nullable = true)
    @JsonIgnore
    private DocumentDetails documentId;



    // ───── Archive Info ─────
    @Column(name = "archived", nullable = false)
    private Boolean archive = false;

    @Column(name = "archive_requestId")
    private Long requestId;

    @Column(name = "archival_status")
    private String archivalStatus;

    // ✅ Fixed: VARCHAR(500) instead of LOB/TEXT
    @Column(name = "archive_failure_reason", length = 500)
    private String failedReason;

    @Column(name = "restored", nullable = false)
    private Boolean restored = false;

    @Column(name = "restored_count")
    private Long restoredCount = 0L;

    @Column(name = "restored_status")
    private String restoredStatus;


    // ───── Approval Info ─────
    @Column(name = "approved_on")
    private Timestamp approvedOn;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // ───── Audit Fields ─────
    @Column(name = "created_on", nullable = false, updatable = false)
    private Timestamp createdOn;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_on")
    private Timestamp updatedOn;

    @Column(name = "updated_by")
    private String updatedBy;


    // file data
    private String mimeType;
    private String fileType;
    private String fileSizeBytes;
    private String fileSizeHuman;
    private Integer pageCounts;

    // ───── Evidence Metadata (moved here from document_header — per file) ─────
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "evidence_type_id")
    private EvidenceTypeMaster evidenceTypeId;

    @Lob
    @Column(name = "evidence_description")
    private String evidenceDescription;

    // ───── Assignment Information ─────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_division_id")
    @JsonIgnore
    private DepartmentMaster department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    @JsonIgnore
    private Employee empId;

    @Lob
    @Column(name = "assignment_remark")
    private String assignmentRemark;




    // LTO Fields
    @Column(name = "lto_archived")
    private Boolean ltoArchived = false;

    @Column(name = "lto_archived_on")
    private LocalDateTime ltoArchivedOn;

    @Column(name = "lto_restored_on")
    private LocalDateTime ltoRestoredOn;

    @Column(name = "lto_tape_path")
    private String ltoTapePath;

    @Column(name = "lto_status")
    private String ltoStatus = "PENDING";

    @Column(name = "lto_job_id")
    private String ltoJobId;

    @Column(name = "lto_error", length = 500)
    private String ltoError;

    @Column(name = "lto_checksum")
    private String ltoChecksum;

    @Column(name = "archived_path")
    private String archivedPath;

    @Column(name = "lto_failure_reason")
    private String ltoFailureReason;

    @Column(name = "lto_restore_jobid")
    private String restoreJobId;

    @Column(name = "cartridge_id")
    private String cartridgeId;

}