package com.dmsBackend.response;

import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.entity.DepartmentMaster;
import com.dmsBackend.entity.RoleMaster;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private Integer id;
    private String employeeId;
    private String name;
    private String email;
    private BranchMaster branch;
    private DepartmentMaster department;
    private String mobile;
    private boolean isActive;
    private Timestamp createdOn;
    private Timestamp updatedOn;
    private RoleMaster role;
    private EmployeeDTO createdBy; // Nested DTO for createdBy
    private EmployeeDTO updatedBy; // Nested DTO for updatedBy
}
