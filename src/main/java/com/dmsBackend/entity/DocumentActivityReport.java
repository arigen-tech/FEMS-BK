package com.dmsBackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "DOCUMENT_ACTIVITY_REPORT")
@Getter
@Setter
public class DocumentActivityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REPORT_ID")
    private Long reportId;

    // -------- Parent (Document Header) --------
    @Column(name = "DOCUMENT_HD_ID")
    private Long documentHdId;

    @Column(name = "DOCUMENT_DT_ID")
    private Long documentDtId;

    @Column(name = "DEPARTMENT_ID")
    private Long departmentId;

    @Column(name = "BRANCH_ID")
    private Long branchId;

    @Column(name = "CATEGORY_ID")
    private Long categoryId;

    @Column(name = "EMP_ID")
    private Long empId;

    @Column(name = "DOCUMENT_NAME")
    private String documentName;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "SUBJECT")
    private String subject;

    @Column(name = "CATEGORY")
    private String category;

    // -------- Child (Document Detail) --------
    @Column(name = "FILE_NAME")
    private String fileName;

    @Column(name = "VERSION")
    private String version;

    @Column(name = "FILE_SIZE")
    private String fileSize;

    // -------- Action Info --------
    @Enumerated(EnumType.STRING)
    @Column(name = "ACTION_TYPE")
    private ActionTypeForReport actionType;// UPLOAD, DOWNLOAD, ARCHIVE, etc.

    @Column(name = "ACTION_DATE")
    private LocalDateTime actionDate;

    @Column(name = "ACTION_BY")
    private String actionBy;

    @Column(name = "USER_ROLE")
    private String userRole;

    // -------- Tracking --------
    @Column(name = "IP_ADDRESS")
    private String ipAddress;

    @Column(name = "LOCATION")
    private String location;

    @Column(name = "JOB_ID")
    private String jobId;

    @Column(name = "REMARKS")
    private String remarks;

    // -------- Status --------
    @Column(name = "STATUS")
    private String status;

    @Column(name = "RETENTION_UNTIL")
    private LocalDate retentionUntil;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    // -------- Constructors --------
    public DocumentActivityReport() {
    }

}
