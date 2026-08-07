package com.dmsBackend.response;

import com.dmsBackend.entity.RetentionPolicy;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class NewRetentionPolicyRequest {
    private String description;
    private LocalDateTime fromdate;
    private LocalDateTime todate;
    private LocalDate retentionDate;
    private LocalTime retentionTime;
    private Boolean isActive;
    private RetentionPolicy.PolicyType policyType;
    private Long departmentId;
    private Long branchId;
    private Long categoryId;

}