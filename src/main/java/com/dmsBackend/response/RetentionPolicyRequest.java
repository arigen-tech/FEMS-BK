package com.dmsBackend.response;

import com.dmsBackend.entity.RetentionPolicy;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RetentionPolicyRequest {
    private String description;
    private LocalDate retentionDate;
    private LocalTime retentionTime;
    private Boolean isActive;
    private RetentionPolicy.PolicyType policyType;
    private Long departmentId;
    private Long branchId;
    private Long categoryId;


}