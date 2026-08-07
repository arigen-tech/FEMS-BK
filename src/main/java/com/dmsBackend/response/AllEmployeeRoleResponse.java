package com.dmsBackend.response;

import com.dmsBackend.entity.EmployeeRole;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class AllEmployeeRoleResponse {
    private Integer employeeId;
    private String name;
    private String branchName;
    private String departmentName;
    private String roleName;

    // DTO specifically for EmployeeRole to control serialization
    @Getter
    @Setter
    public static class EmployeeRoleDTO {
        private Integer id;
        private Integer empId;
        private Integer roleId;
        private String roleName; // Added role name

        private Integer roleCode;
        private boolean isActive;
        private Integer updatedById;
        private String updatedByName; // Added updatedByName
        private Integer createdById;
        private String createdByName; // Added createdByName
        private Timestamp createdOn;
        private Timestamp updatedOn;

        // Constructor to convert EmployeeRole to DTO
        public EmployeeRoleDTO(EmployeeRole employeeRole) {
            this.id = employeeRole.getId();
            this.empId = employeeRole.getEmpId() != null ? employeeRole.getEmpId().getId() : null;
            this.roleId = employeeRole.getRoleId() != null ? employeeRole.getRoleId().getId() : null;
            // Added role name extraction
            this.roleName = employeeRole.getRoleId() != null ? employeeRole.getRoleId().getRole() : null;
            this.roleCode = employeeRole.getRoleId().getRoleCode();
            this.isActive = employeeRole.isActive();
            this.updatedById = employeeRole.getUpdatedBy() != null ? employeeRole.getUpdatedBy().getId() : null;
            this.updatedByName = employeeRole.getUpdatedBy() != null ? employeeRole.getUpdatedBy().getName() : null; // Set updatedByName
            this.createdById = employeeRole.getCreatedBy() != null ? employeeRole.getCreatedBy().getId() : null;
            this.createdByName = employeeRole.getCreatedBy() != null ? employeeRole.getCreatedBy().getName() : null; // Set createdByName
            this.createdOn = employeeRole.getCreatedOn();
            this.updatedOn = employeeRole.getUpdatedOn();
        }
    }

    private List<EmployeeRoleDTO> employeeRoles;
    private String status;
    private Timestamp createdOn;
    private Timestamp updatedOn;
    private Integer createdBy;
    private String createdByName; // Added createdByName
    private Integer updatedBy;
    private String updatedByName; // Added updatedByName
    private String mobile;
    private String email;

    // Default constructor
    public AllEmployeeRoleResponse() {}

    // Constructor with parameters
    public AllEmployeeRoleResponse(
            Integer employeeId,
            String name,
            String branchName,
            String departmentName,
            String roleName,
            List<EmployeeRoleDTO> employeeRoles,
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
        this.name = name;
        this.branchName = branchName;
        this.departmentName = departmentName;
        this.roleName = roleName;
        this.employeeRoles = employeeRoles;
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

    // Method to set EmployeeRoles and convert to DTOs
    public void setEmployeeRoles(List<EmployeeRole> employeeRoles) {
        this.employeeRoles = employeeRoles.stream()
                .map(EmployeeRoleDTO::new)
                .collect(Collectors.toList());
    }
}