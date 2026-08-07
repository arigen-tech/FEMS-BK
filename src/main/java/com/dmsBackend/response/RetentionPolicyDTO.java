package com.dmsBackend.response;

import com.dmsBackend.entity.RetentionPolicy;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class RetentionPolicyDTO {
    private Long id;
    private String description;
    private LocalDateTime fromdate;
    private LocalDateTime todate;
    private LocalDate retentionDate;
    private LocalTime retentionTime;
    private Boolean isActive;
    private RetentionPolicy.PolicyType policyType;
    private Integer departmentId;
    private Integer branchId;
    private Integer categoryId;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;

    // Helper method to get combined date and time
    public LocalDateTime getRetentionDateTime() {
        if (retentionDate == null || retentionTime == null) {
            return null;
        }
        return LocalDateTime.of(retentionDate, retentionTime);
    }
}