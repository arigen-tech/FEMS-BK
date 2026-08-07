package com.dmsBackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(
        name = "document_header",
        indexes = {
                @Index(name = "idx_doc_no", columnList = "doc_no"),
                @Index(name = "idx_doc_title", columnList = "doc_title"),
                @Index(name = "idx_doc_subject", columnList = "doc_sub"),
                @Index(name = "idx_doc_category_id", columnList = "category_id"),
                @Index(name = "idx_doc_employee_id", columnList = "employee_id"),
                @Index(name = "idx_doc_approval_status", columnList = "approval_status"),
                @Index(name = "idx_doc_is_active", columnList = "is_active")
        }
)
@JsonIgnoreProperties({
        "branch",
        "department",
        "language",
        "profileImage",
        "role",
        "hibernateLazyInitializer", "handler"
})
public class DocumentHeader {

    // ───── Primary Key ─────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ───── Core Document Info ─────
    @Column(name = "doc_title", nullable = false)
    private String title;

    @Column(name = "doc_no", nullable = false)
    private String fileNo;

    @Column(name = "doc_sub", nullable = false)
    private String subject;

    // ------------ approval flag --------------
    @Column(name = "if_approved")
    private Boolean ifApproved = false;

    @Column(name = "if_rejected")
    private Boolean ifRejected = false;

    @Column(name = "if_pending")
    private Boolean ifPending = true;

    @Column(name = "if_external")
    private Boolean externalApiFlag = false;

    // ───── Relations ─────
    @ManyToOne
//    @JsonIgnore
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private CategoryMaster categoryMaster;

    @ManyToOne
//    @JsonIgnore
    @JoinColumn(name = "branch_id", referencedColumnName = "id", nullable = false)
    private BranchMaster branchMaster;

    @ManyToOne
//    @JsonIgnore
    @JoinColumn(name = "department_id", referencedColumnName = "id", nullable = false)
    private DepartmentMaster departmentMaster;

    @ManyToOne
//    @JsonIgnore
    @JoinColumn(name = "employee_id", referencedColumnName = "id", nullable = false)
    private Employee employee;

    @OneToMany(mappedBy = "documentHeader", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<DocumentDetails> documentDetails;

    @OneToMany(
            mappedBy = "documentHeader",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @JsonManagedReference
    private List<DocumentMetadata> metadataList = new ArrayList<>();



    // ───── Approval Info ─────
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private DocApprovalStatus approvalStatus = DocApprovalStatus.PENDING;

    // ───── Audit Fields ─────
    @Column(name = "created_on", nullable = false, updatable = false)
    private Timestamp createdOn;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_on")
    private Timestamp updatedOn;

    @Column(name = "updated_by")
    private String updatedBy;

    // ───── QR & Active Flag ─────
    @Column(name = "qr_path")
    private String qrPath;

    @Column(name = "is_active", nullable = false)
    private boolean active;



    // LTO Status Fields
    @Column(name = "lto_archived")
    private Boolean ltoArchived = false;

    @Column(name = "lto_archived_on")
    private LocalDateTime ltoArchivedOn;

    @Column(name = "lto_job_id")
    private String ltoJobId;

    @Column(name = "lto_tape_barcode")
    private String ltoTapeBarcode;

    @Column(name = "lto_status")
    private String ltoStatus = "PENDING"; // PENDING, ARCHIVING, ARCHIVED, FAILED

    @Column(name = "lto_error")
    private String ltoError;

}
