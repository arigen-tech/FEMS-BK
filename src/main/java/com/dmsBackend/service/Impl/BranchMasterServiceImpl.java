package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.Helper;
import com.dmsBackend.repository.BranchMasterRepository;
import com.dmsBackend.service.BranchMasterService;
import com.dmsBackend.utils.AuditLogUtil;
import com.dmsBackend.utils.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class BranchMasterServiceImpl implements BranchMasterService {

    @Autowired
    private BranchMasterRepository branchMasterRepository;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private AuditLogUtil auditLogUtil;

    // ======================= SAVE =======================
    @Override
    public BranchMaster saveBranchMaster(BranchMaster branchMaster, HttpServletRequest request) {

        log.info("API CALL → Save Branch | name={}", branchMaster.getName());

        branchMaster.setCreatedOn(Helper.getCurrentTimeStamp());
        branchMaster.setUpdatedOn(Helper.getCurrentTimeStamp());

        try {
            BranchMaster savedBranch = branchMasterRepository.save(branchMaster);

            log.info("SUCCESS → Branch Saved | id={} name={}",
                    savedBranch.getId(), savedBranch.getName());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Branches",
                    "Create",
                    "Success",
                    savedBranch.getId(),
                    savedBranch.getName(),
                    savedBranch.getId(),
                    Map.of("name", savedBranch.getName(), "address", savedBranch.getAddress()),
                    request
            );

            return savedBranch;

        } catch (Exception ex) {

            log.info("FAILED → Save Branch | name={} reason={}",
                    branchMaster.getName(), ex.getMessage());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Branches",
                    "Create",
                    "Failure",
                    null,
                    branchMaster.getName(),
                    null,
                    Map.of("error", ex.getMessage()),
                    request
            );
            throw ex;
        }
    }

    // ======================= UPDATE =======================
    @Override
    public BranchMaster updateBranchMaster(BranchMaster branchMaster, Integer id, HttpServletRequest request) {

        log.info("API CALL → Update Branch | id={}", id);

        Optional<BranchMaster> optional = branchMasterRepository.findById(id);

        if (optional.isPresent()) {

            BranchMaster existing = optional.get();

            log.info("Updating Branch | id={} oldName={} newName={}",
                    id, existing.getName(), branchMaster.getName());

            Map<String, Object> previousData = Map.of(
                    "name", existing.getName(),
                    "address", existing.getAddress()
            );

            existing.setName(branchMaster.getName());
            existing.setAddress(branchMaster.getAddress());
            existing.setUpdatedOn(Helper.getCurrentTimeStamp());

            BranchMaster updated = branchMasterRepository.save(existing);

            log.info("SUCCESS → Branch Updated | id={} name={}",
                    updated.getId(), updated.getName());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Branches",
                    "Update",
                    "Success",
                    updated.getId(),
                    updated.getName(),
                    updated.getId(),
                    previousData,
                    request
            );

            return updated;
        }

        log.info("FAILED → Update Branch | id={} reason=Not Found", id);

        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),
                "Branches",
                "Update",
                "Failure",
                id,
                null,
                id,
                Map.of("error", "BranchMaster not found"),
                request
        );

        throw new ResourceNotFoundException("BranchMaster not found", "Id", id);
    }

    // ======================= DELETE =======================
    @Override
    public void deleteByIdBranchMaster(Integer id) {

        log.info("API CALL → Delete Branch | id={}", id);

        branchMasterRepository.deleteById(id);

        log.info("SUCCESS → Branch Deleted | id={}", id);
    }

    // ======================= FIND ALL =======================
    @Override
    public List<BranchMaster> findAllBranchMaster() {

        log.info("API CALL → Get All Branches");

        return branchMasterRepository.findAllBranchMasterOrdered();
    }

    // ======================= FIND ACTIVE =======================
    @Override
    public List<BranchMaster> findAllActiveBranchMaster(Integer isActive) {

        log.info("API CALL → Get Active Branches | isActive={}", isActive);

        return branchMasterRepository.findByIsActive(isActive);
    }

    // ======================= FIND BY ID =======================
    @Override
    public Optional<BranchMaster> findBranchMasterById(Integer id) {

        log.info("API CALL → Get Branch By ID | id={}", id);

        return branchMasterRepository.findById(id);
    }

    public BranchMaster findByIdBran(Integer id) {

        log.info("API CALL → Get Branch (Strict) | id={}", id);

        return branchMasterRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("FAILED → Get Branch | id={} reason=Not Found", id);
                    return new ResourceNotFoundException("Branch not found", "Id", id);
                });
    }

    // ======================= STATUS UPDATE =======================
    @Override
    public BranchMaster updateStatus(Integer id, Integer isActive, HttpServletRequest request) {

        log.info("API CALL → Branch Status Update | id={} newStatus={}", id, isActive);

        BranchMaster branchMaster = branchMasterRepository.findById(id)
                .orElseThrow(() -> {

                    log.info("FAILED → Status Update | id={} reason=Not Found", id);

                    auditLogUtil.logAction(
                            currentUser.getCurrentEmployeeOrThrow(),
                            "Branches",
                            "StatusUpdate",
                            "Failure",
                            id,
                            null,
                            id,
                            Map.of("error", "BranchMaster not found"),
                            request
                    );

                    return new ResourceNotFoundException("BranchMaster", "id", id);
                });

        Integer oldStatus = branchMaster.getIsActive();

        branchMaster.setIsActive(isActive);
        branchMaster.setUpdatedOn(Helper.getCurrentTimeStamp());

        BranchMaster updatedBranch = branchMasterRepository.save(branchMaster);

        log.info("SUCCESS → Branch Status Updated | id={} oldStatus={} newStatus={}",
                id, oldStatus, isActive);

        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),
                "Branches",
                "StatusUpdate",
                "Success",
                updatedBranch.getId(),
                updatedBranch.getName(),
                updatedBranch.getId(),
                Map.of("isActive", oldStatus),
                request
        );

        return updatedBranch;
    }
}
