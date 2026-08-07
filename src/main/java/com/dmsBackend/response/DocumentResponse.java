package com.dmsBackend.response;

import com.dmsBackend.entity.DocApprovalStatus;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentResponse {
    private String title;
    private String fileNo;
    private String subject;
    private String version;
    private String categoryName;
    private String branchName;
    private String departmentName;

    private Integer employeeId;
    private Timestamp createdOn;
    private String approvalStatus; // DocApprovalStatus display name
    private Integer documentDetailsLength;

    // Constructor

    public DocumentResponse(String title, String fileNo, String subject, String categoryName,
                            String branchName, String departmentName, Timestamp createdOn,
                            DocApprovalStatus approvalStatus, Long documentDetailsLength) {
        this.title = title;
        this.fileNo = fileNo;
        this.subject = subject;
        this.categoryName = categoryName;
        this.branchName = branchName;
        this.departmentName = departmentName;
        this.createdOn = createdOn;
        this.approvalStatus = approvalStatus.getDisplayName();
        this.documentDetailsLength = documentDetailsLength != null ? documentDetailsLength.intValue() : 0;
    }



    public DocumentResponse(
            String title,
            String fileNo,
            String subject,
            String categoryName,
            String branchName,
            Integer employeeId,
            String departmentName,
            Timestamp createdOn,
            DocApprovalStatus approvalStatus, // Accept enum
            Integer documentDetailsLength) {
        this.title = title;
        this.fileNo = fileNo;
        this.subject = subject;
        this.categoryName = categoryName;
        this.branchName = branchName;
        this.employeeId = employeeId;
        this.departmentName = departmentName;
        this.createdOn = createdOn;
        this.approvalStatus = approvalStatus.getDisplayName(); // Map enum to display name
        this.documentDetailsLength = documentDetailsLength;
    }



    public void setApprovalStatus(DocApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus.getDisplayName(); // Map Enum to Display Name
    }
}
