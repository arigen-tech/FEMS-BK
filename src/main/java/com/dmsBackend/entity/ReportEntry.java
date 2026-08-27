package com.dmsBackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "report_entry")
public class ReportEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_header_id", nullable = false)
    private DocumentHeader documentHeader;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "examination_start_date")
    private Date examinationStartDate;

    @Column(name = "examination_end_date")
    private Date examinationEndDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "examination_method_id")
    private ExaminationMethodMaster examinationMethod;

    @Lob
    @Column(name = "observations")
    private String observations;

    @Lob
    @Column(name = "scientific_opinion")
    private String scientificOpinion;

    @Lob
    @Column(name = "examination_remarks")
    private String examinationRemarks;

    @Column(name = "report_date")
    private Date reportDate;

    @Column(name = "report_title")
    private String reportTitle;

    @Lob
    @Column(name = "report_summary")
    private String reportSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(name = "scientific_report_path")
    private String scientificReportPath;

    @Column(name = "created_on")
    private Timestamp createdOn;

    @Column(name = "updated_on")
    private Timestamp updatedOn;

    @Column(name = "submitted_on")
    private Timestamp submittedOn;

    // REVIEW FIELDS
    @Column(name = "review_status")
    private String reviewStatus = "PENDING"; // PENDING, APPROVED, REJECTED, REFERRED

    @Column(name = "reviewed_by")
    private Integer reviewedBy;

    @Column(name = "reviewed_on")
    private Timestamp reviewedOn;

    @Column(name = "review_comments")
    private String reviewComments;

    @Column(name = "final_report_path")
    private String finalReportPath;

    // REFERRAL FIELDS
    @Column(name = "referral_status")
    private String referralStatus;

    @Column(name = "referred_to_lab")
    private String referredToLab;

    @Column(name = "referred_on")
    private Timestamp referredOn;

    @Column(name = "referral_reason")
    private String referralReason;

    @Column(name = "referred_from_lab")
    private String referredFromLab;

    // ───── Dispatch Fields ─────
    @Column(name = "dispatch_status")
    private String dispatchStatus; // PENDING, DISPATCHED

    @Column(name = "dispatch_date")
    private Date dispatchDate;

    @Column(name = "dispatch_reference_no")
    private String dispatchReferenceNo;

    @Column(name = "recipient")
    private String recipient;

    @Column(name = "dispatch_mode")
    private String dispatchMode;

    @Column(name = "dispatch_document_path")
    private String dispatchDocumentPath;

    @Lob
    @Column(name = "dispatch_remarks")
    private String dispatchRemarks;

    @Column(name = "notify_email")
    private Boolean notifyEmail = false;

    @Column(name = "notify_sms")
    private Boolean notifySms = false;

    @Column(name = "dispatched_by")
    private Integer dispatchedBy;

    @Column(name = "dispatched_on")
    private Timestamp dispatchedOn;

    @OneToMany(mappedBy = "reportEntry", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ReportEntryAttachment> attachments = new ArrayList<>();

    public enum ReportStatus {
        DRAFT, SUBMITTED
    }
}