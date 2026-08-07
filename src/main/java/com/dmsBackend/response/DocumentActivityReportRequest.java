package com.dmsBackend.response;

import com.dmsBackend.entity.ActionTypeForReport;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DocumentActivityReportRequest {
    private Long branchId;
    private Long departmentId;
    private Long categoryId;
    private Long empId;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private List<ActionTypeForReport> actionTypes;
}
