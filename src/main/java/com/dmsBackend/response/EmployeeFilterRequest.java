package com.dmsBackend.response;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDate;

@Data
public class EmployeeFilterRequest {
    private Integer departmentMasterBranchId;
    private Integer departmentMasterId;
    private Boolean status;
    private Timestamp startDate;
    private Timestamp endDate;
    private String docType;
}

