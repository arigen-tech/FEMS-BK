package com.dmsBackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        "hibernateLazyInitializer",
        "handler"
})
public class DocumentHeader {

    // =========================================================
    // PRIMARY KEY
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    // =========================================================
    // CORE DOCUMENT INFORMATION
    // =========================================================

    @Column(name = "doc_title", nullable = false)
    private String title;

    @Column(name = "doc_no", nullable = false)
    private String fileNo;

    @Column(name = "doc_sub", nullable = false)
    private String subject;


    // =========================================================
    // APPROVAL FLAGS
    // =========================================================

    @Column(name = "if_approved")
    private Boolean ifApproved = false;

    @Column(name = "if_rejected")
    private Boolean ifRejected = false;

    @Column(name = "if_pending")
    private Boolean ifPending = true;

    @Column(name = "if_external")
    private Boolean externalApiFlag = false;


    // =========================================================
    // BASIC MASTER RELATIONS
    // =========================================================

    @ManyToOne
//    @JsonIgnore
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private CategoryMaster categoryMaster;


    @ManyToOne
    @JoinColumn(
            name = "branch_id",
            referencedColumnName = "id",
            nullable = false
    )
    private BranchMaster branchMaster;


    @ManyToOne
    @JoinColumn(
            name = "department_id",
            referencedColumnName = "id",
            nullable = false
    )
    private DepartmentMaster departmentMaster;


    @ManyToOne
    @JoinColumn(
            name = "employee_id",
            referencedColumnName = "id",
            nullable = false
    )
    private Employee employee;


    // =========================================================
    // DOCUMENT DETAILS
    // =========================================================

    @OneToMany(
            mappedBy = "documentHeader",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL
    )
    @JsonManagedReference
    private List<DocumentDetails> documentDetails = new ArrayList<>();


    // =========================================================
    // DOCUMENT METADATA
    // =========================================================

    @OneToMany(
            mappedBy = "documentHeader",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @JsonManagedReference
    private List<DocumentMetadata> metadataList = new ArrayList<>();


    // =========================================================
    // APPROVAL INFORMATION
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "approval_status",
            nullable = false
    )
    private DocApprovalStatus approvalStatus = DocApprovalStatus.PENDING;


    // =========================================================
    // AUDIT INFORMATION
    // =========================================================

    @Column(
            name = "created_on",
            nullable = false,
            updatable = false
    )
    private Timestamp createdOn;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_on")
    private Timestamp updatedOn;

    @Column(name = "updated_by")
    private String updatedBy;


    // =========================================================
    // QR & ACTIVE FLAG
    // =========================================================

    @Column(name = "qr_path")
    private String qrPath;

    @Column(name = "is_active", nullable = false)
    private boolean active;


    // =========================================================
    // LTO STATUS
    // =========================================================

    @Column(name = "lto_archived")
    private Boolean ltoArchived = false;

    @Column(name = "lto_archived_on")
    private LocalDateTime ltoArchivedOn;

    @Column(name = "lto_job_id")
    private String ltoJobId;

    @Column(name = "lto_tape_barcode")
    private String ltoTapeBarcode;

    @Column(name = "lto_status")
    private String ltoStatus = "PENDING";

    @Column(name = "lto_error")
    private String ltoError;


    // =========================================================
    // CASE INFORMATION
    // =========================================================

    @Column(name = "case_id")
    private String caseId;

    @Column(name = "fir_number")
    private String firNumber;

    @Column(name = "fir_date")
    private Timestamp firDate;


    // =========================================================
    // CASE TYPE MASTER
    // =========================================================

    @ManyToOne
    @JoinColumn(
            name = "case_type_id",
            referencedColumnName = "id"
    )
    private CaseTypeMaster caseType;


    // =========================================================
    // CRIME TYPE MASTER
    // =========================================================

    @ManyToOne
    @JoinColumn(
            name = "crime_type_id",
            referencedColumnName = "id"
    )
    private CrimeTypeMaster crimeType;


    // =========================================================
    // STATE MASTER
    // =========================================================

    @ManyToOne
    @JoinColumn(
            name = "state_id",
            referencedColumnName = "id"
    )
    private StateMaster state;


    // =========================================================
    // DISTRICT MASTER
    // =========================================================

    @ManyToOne
    @JoinColumn(
            name = "district_id",
            referencedColumnName = "id"
    )
    private DistrictMaster district;


    // =========================================================
    // CITY MASTER
    // =========================================================

    @ManyToOne
    @JoinColumn(
            name = "city_id",
            referencedColumnName = "id"
    )
    private CityMaster city;


    // =========================================================
    // CASE DETAILS
    // =========================================================

    @Column(name = "police_station")
    private String policeStation;

    @Column(name = "investigating_officer")
    private String investigatingOfficer;

    @Column(name = "court_reference")
    private String courtReference;


    // =========================================================
    // PRIORITY MASTER
    // =========================================================

    @ManyToOne
    @JoinColumn(
            name = "priority_id",
            referencedColumnName = "id"
    )
    private PriorityMaster priority;


    @Column(name = "date_of_incident")
    private Timestamp dateOfIncident;

    @Column(
            name = "incident_location",
            length = 500
    )
    private String incidentLocation;


    // =========================================================
    // EVIDENCE INFORMATION
    // Header-level only
    // Evidence type/description are in DocumentDetails
    // =========================================================

    @Column(name = "evidence_id")
    private String evidenceId;

    @Column(name = "exhibit_number")
    private String exhibitNumber;


    // =========================================================
    // APPROVAL TRACKING
    // =========================================================

    @Column(name = "approval_status_by")
    private String approvalStatusBy;

    @Column(name = "approval_status_on")
    private Timestamp approvalStatusOn;

    @Transient
    private DocumentForwardingAuthority forwardingAuthority;

    public DocumentForwardingAuthority getForwardingAuthority() {
        return forwardingAuthority;
    }

    public void setForwardingAuthority(DocumentForwardingAuthority forwardingAuthority) {
        this.forwardingAuthority = forwardingAuthority;
    }
}