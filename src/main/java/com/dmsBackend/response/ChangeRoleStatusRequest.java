package com.dmsBackend.response;

import java.time.LocalDateTime;

public class ChangeRoleStatusRequest {
    private Integer empId;
    private Integer roleId;
    private boolean status;
    private LocalDateTime updatedOnDate;
    private Integer updatedById;
    private String updatedByName;

    // Getters and Setters
    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedOnDate() {
        return updatedOnDate;
    }

    public void setUpdatedOnDate(LocalDateTime updatedOnDate) {
        this.updatedOnDate = updatedOnDate;
    }

    public Integer getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(Integer updatedById) {
        this.updatedById = updatedById;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }
}