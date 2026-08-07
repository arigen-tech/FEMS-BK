package com.dmsBackend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Getter
@Setter
@Table(name = "audit_log")
public class DocumentsAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "employee_name", length = 100)
    private String employeeName;

    @Column(name = "form_name", nullable = false, length = 100)
    private String formName;

    @Column(name = "activity", nullable = false, length = 100)
    private String activity;

    @Column(name = "document_id")
    private Integer documentId;

    @Column(name = "document_name", length = 255)
    private String documentName;

    @Column(name = "document_details_id")
    private Integer documentDetailsId;

    @Column(name = "branch_id")
    private Integer branchId;

    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "status", length = 20)
    private String status; // Success / Failure

//    @JdbcTypeCode(SqlTypes.JSON)   // ✅ Hibernate 6+ में JSON mapping
//    @Column(name = "details", columnDefinition = "jsonb")
//    private Map<String, Object> detailsJson;

    // ✅ FIXED: MySQL JSON (NOT jsonb)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "json")
    private Map<String, Object> detailsJson;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", updatable = false, insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "login_at")
    private LocalDateTime loginAt;

    @Column(name = "uniqueId")
    private Integer uniqueId;


}
