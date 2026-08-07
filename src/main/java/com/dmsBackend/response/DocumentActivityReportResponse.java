package com.dmsBackend.response;

import com.dmsBackend.entity.ActionTypeForReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class DocumentActivityReportResponse {
    private Long reportId;

    private Long documentHdId;
    private Long documentDtId;

    private Long departmentId;
    private Long branchId;
    private Long categoryId;
    private Long empId;

    private String documentName;
    private String title;
    private String subject;
    private String category;

    private String fileName;
    private String version;
    private String fileSize;

    private ActionTypeForReport actionType;
    private LocalDateTime actionDate;
    private String actionBy;
    private String userRole;

    private String ipAddress;
    private String location;
    private String jobId;
    private String remarks;

    private String status;
    private LocalDate retentionUntil;
    private LocalDateTime createdAt;
}
