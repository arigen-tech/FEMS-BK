package com.dmsBackend.service;

import com.dmsBackend.entity.DepartmentMaster;
import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.response.DepartmentResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

public interface DepartmentMasterService {
    DepartmentMaster saveDepartmentMaster(DepartmentMaster departmentMaster, HttpServletRequest request);
    DepartmentMaster updateDepartmentMaster(DepartmentMaster departmentMaster,Integer id, HttpServletRequest request);
    void deleteByIdDepartmentMaster(Integer id);
    List<DepartmentResponse> findAllDepartmentMaster();
    List<DepartmentMaster> findDepartmentMasterByBranch(Integer branchId);

    List<DepartmentMaster> findAllActiveDepartmentMaster(Integer isActive);

    Optional<DepartmentMaster> findDepartmentMasterById(Integer id);

    DepartmentMaster findByIdDep(Integer id);
    DepartmentMaster updateStatusDepartment(Integer id, Integer isApproved,HttpServletRequest request);

    DepartmentMaster findById(Integer id);
}