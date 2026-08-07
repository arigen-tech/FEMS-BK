package com.dmsBackend.response;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class EmployeeRoleResponse {
    private Integer employeeId;
    private String employeeName;
    private String branchName;
    private String departmentName;
    private String employeeRole; // Single role name
    private List<String> roleNamesList; // List of role names
    private String status;
    private Timestamp createdOn;
    private Timestamp updatedOn;
    private Integer createdBy;
    private String createdByName; // Added createdByName
    private Integer updatedBy;
    private String updatedByName; // Added updatedByName
    private String mobile;
    private String email;

    // Constructor matching the parameters from your service method
    public EmployeeRoleResponse(
            Integer employeeId,
            String employeeName,
            String branchName,
            String departmentName,
            String employeeRole,
            List<String> roleNamesList,
            String status,
            Timestamp createdOn,
            Timestamp updatedOn,
            Integer createdBy,
            String createdByName, // New parameter
            Integer updatedBy,
            String updatedByName, // New parameter
            String mobile,
            String email
    ) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.branchName = branchName;
        this.departmentName = departmentName;
        this.employeeRole = employeeRole;
        this.roleNamesList = roleNamesList;
        this.status = status;
        this.createdOn = createdOn;
        this.updatedOn = updatedOn;
        this.createdBy = createdBy;
        this.createdByName = createdByName; // Set createdByName
        this.updatedBy = updatedBy;
        this.updatedByName = updatedByName; // Set updatedByName
        this.mobile = mobile;
        this.email = email;
    }
}