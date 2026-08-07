package com.dmsBackend.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data

public class DocumentsAuditLogRequest {
    private String formName;
    private String activity;
    private String status;
    private LocalDateTime loginAt;
    private String ipAddress;
    private Map<String, Object> detailsJson; // 👈 stays as Map

    private Integer documentId;
    private Integer documentDetailsId; // 👈 single, not list
    private String documentName;
    private Integer employeeId;
    private Integer uniqueId;



}
