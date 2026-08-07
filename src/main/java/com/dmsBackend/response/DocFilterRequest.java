package com.dmsBackend.response;

import com.dmsBackend.entity.DocApprovalStatus;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class DocFilterRequest {
    private Integer categoryId;
    private DocApprovalStatus approvalStatus;
    private Timestamp startDate;
    private Timestamp endDate;
    private Integer branchId;
    private Integer departmentId;
    private Integer employeeId;
    private String docType;
}
