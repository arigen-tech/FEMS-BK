package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.entity.DepartmentMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.Helper;
import com.dmsBackend.repository.BranchMasterRepository;
import com.dmsBackend.repository.DepartmentMasterRepository;
import com.dmsBackend.response.DepartmentResponse;
import com.dmsBackend.service.DepartmentMasterService;
import com.dmsBackend.utils.AuditLogUtil;
import com.dmsBackend.utils.CurrentUser;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class DepartmentMasterServiceImpl implements DepartmentMasterService {

    @Autowired
    DepartmentMasterRepository departmentMasterRepository;

    @Autowired
    BranchMasterRepository branchMasterRepository;

    @Autowired
    CurrentUser currentUser;

    @Autowired
    AuditLogUtil auditLogUtil;

    // ======================= SAVE =======================
    @Override
    public DepartmentMaster saveDepartmentMaster(DepartmentMaster departmentMaster, HttpServletRequest request) {

        log.info("API CALL → Save Department | name={} branchId={}",
                departmentMaster.getName(), departmentMaster.getBranch().getId());

        BranchMaster branchMaster = branchMasterRepository.findById(departmentMaster.getBranch().getId())
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        departmentMaster.setBranch(branchMaster);

        departmentMaster.setCreatedOn(Helper.getCurrentTimeStamp());
        departmentMaster.setUpdatedOn(Helper.getCurrentTimeStamp());

        try {
            DepartmentMaster savedDepartment = departmentMasterRepository.save(departmentMaster);

            log.info("SUCCESS → Department Saved | id={} name={} branch={}",
                    savedDepartment.getId(), savedDepartment.getName(), savedDepartment.getBranch().getName());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Departments",
                    "Create",
                    "Success",
                    savedDepartment.getId(),
                    savedDepartment.getName(),
                    savedDepartment.getId(),
                    Map.of("name", savedDepartment.getName(), "branch", savedDepartment.getBranch().getName()),
                    request
            );

            return savedDepartment;

        } catch (Exception ex) {

            log.info("FAILED → Save Department | name={} reason={}",
                    departmentMaster.getName(), ex.getMessage());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Departments",
                    "Create",
                    "Failure",
                    null,
                    departmentMaster.getName(),
                    null,
                    Map.of("error", ex.getMessage()),
                    request
            );

            throw ex;
        }
    }

    // ======================= UPDATE =======================
    @Override
    public DepartmentMaster updateDepartmentMaster(DepartmentMaster departmentMaster, Integer id, HttpServletRequest request) {

        log.info("API CALL → Update Department | id={}", id);

        Optional<DepartmentMaster> departmentMaster1 = departmentMasterRepository.findById(id);

        if(departmentMaster1.isPresent()){
            DepartmentMaster existing = departmentMaster1.get();

            log.info("Updating Department | id={} oldName={} newName={} branchId={}",
                    id, existing.getName(), departmentMaster.getName(), departmentMaster.getBranch().getId());

            Map<String, Object> previousData = Map.of(
                    "name", existing.getName(),
                    "branch", existing.getBranch().getName()
            );

            existing.setName(departmentMaster.getName());
            existing.setIsActive(departmentMaster.getIsActive());
            existing.setUpdatedOn(Helper.getCurrentTimeStamp());

            BranchMaster branchMaster = branchMasterRepository.findById(departmentMaster.getBranch().getId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
            existing.setBranch(branchMaster);

            DepartmentMaster updatedDepartment = departmentMasterRepository.save(existing);

            log.info("SUCCESS → Department Updated | id={} name={} branch={}",
                    updatedDepartment.getId(), updatedDepartment.getName(), updatedDepartment.getBranch().getName());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "DEPARTMENTS",
                    "Update",
                    "Success",
                    updatedDepartment.getId(),
                    updatedDepartment.getName(),
                    updatedDepartment.getId(),
                    previousData,
                    request
            );

            return updatedDepartment;
        } else {

            log.info("FAILED → Update Department | id={} reason=Not Found", id);

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "DEPARTMENTS",
                    "Update",
                    "Failure",
                    id,
                    null,
                    id,
                    Map.of("error", "DepartmentMaster not found"),
                    request
            );
            throw new ResourceNotFoundException("DepartmentMaster not found for ", "Id", id);
        }
    }

    // ======================= DELETE =======================
    @Override
    public void deleteByIdDepartmentMaster(Integer id) {

        log.info("API CALL → Delete Department | id={}", id);

        departmentMasterRepository.deleteById(id);

        log.info("SUCCESS → Department Deleted | id={}", id);
    }

    // ======================= FIND ALL =======================
    @Override
    public List<DepartmentResponse> findAllDepartmentMaster() {

        log.info("API CALL → Get All Departments");

        List<DepartmentMaster> departmentMasterList = departmentMasterRepository.findAllDepartmentMasterOrdered();
        List<DepartmentResponse> mainResponse = new ArrayList<>();

        for (DepartmentMaster department : departmentMasterList) {
            DepartmentResponse copy = new DepartmentResponse();
            BeanUtils.copyProperties(department, copy);
            mainResponse.add(copy);
        }

        log.info("SUCCESS → Retrieved {} Departments", mainResponse.size());

        return mainResponse;
    }

    // ======================= FIND BY BRANCH =======================
    @Override
    public List<DepartmentMaster> findDepartmentMasterByBranch(Integer branchId) {

        log.info("API CALL → Get Departments By Branch | branchId={}", branchId);

        List<DepartmentMaster> departments = departmentMasterRepository.findByBranchId(branchId);

        log.info("SUCCESS → Retrieved {} Departments for Branch ID: {}", departments.size(), branchId);

        return departments;
    }

    // ======================= FIND ACTIVE =======================
    @Override
    public List<DepartmentMaster> findAllActiveDepartmentMaster(Integer isActive) {

        log.info("API CALL → Get Active Departments | isActive={}", isActive);

        return departmentMasterRepository.findByIsActive(isActive);
    }

    // ======================= FIND BY ID =======================
    @Override
    public Optional<DepartmentMaster> findDepartmentMasterById(Integer id) {

        log.info("API CALL → Get Department By ID | id={}", id);

        return departmentMasterRepository.findById(id);
    }

    @Override
    public DepartmentMaster findByIdDep(Integer id) {

        log.info("API CALL → Get Department (Strict) | id={}", id);

        return departmentMasterRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("FAILED → Get Department | id={} reason=Not Found", id);
                    return new ResourceNotFoundException("Department not found", "Id", id);
                });
    }

    // ======================= STATUS UPDATE =======================
    @Override
    public DepartmentMaster updateStatusDepartment(Integer id, Integer isApproved, HttpServletRequest request) {

        log.info("API CALL → Department Status Update | id={} newStatus={}", id, isApproved);

        DepartmentMaster departmentMaster = departmentMasterRepository.findById(id)
                .orElseThrow(() -> {

                    log.info("FAILED → Status Update | id={} reason=Not Found", id);

                    auditLogUtil.logAction(
                            currentUser.getCurrentEmployeeOrThrow(),
                            "Departments",
                            "StatusUpdate",
                            "Failure",
                            id,
                            null,
                            id,
                            Map.of("error", "DepartmentMaster not found"),
                            request
                    );

                    return new ResourceNotFoundException("DepartmentMaster", "id", id);
                });

        Integer oldStatus = departmentMaster.getIsActive();

        departmentMaster.setUpdatedOn(Helper.getCurrentTimeStamp());
        departmentMaster.setIsActive(isApproved);

        DepartmentMaster updatedDepartment = departmentMasterRepository.save(departmentMaster);

        log.info("SUCCESS → Department Status Updated | id={} oldStatus={} newStatus={}",
                id, oldStatus, isApproved);

        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),
                "Departments",
                "StatusUpdate",
                "Success",
                updatedDepartment.getId(),
                updatedDepartment.getName(),
                updatedDepartment.getId(),
                Map.of("isActive", oldStatus),
                request
        );

        return updatedDepartment;
    }

    @Override
    public DepartmentMaster findById(Integer id) {

        log.info("API CALL → Find Department By ID (Entity) | id={}", id);

        return departmentMasterRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("FAILED → Find Department | id={} reason=Not Found", id);
                    return new EntityNotFoundException("DepartmentMaster not found for ID: " + id);
                });
    }
}