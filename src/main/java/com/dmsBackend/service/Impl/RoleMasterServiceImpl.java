package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.Helper;
import com.dmsBackend.repository.RoleMasterRepository;
import com.dmsBackend.service.RoleMasterService;
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
public class RoleMasterServiceImpl implements RoleMasterService {

    @Autowired
    private RoleMasterRepository roleMasterRepository;

    @Autowired
    CurrentUser currentUser;

    @Autowired
    AuditLogUtil auditLogUtil;

    // ======================= SAVE =======================
    @Override
    public RoleMaster saveRoleMaster(RoleMaster roleMaster, HttpServletRequest request) {

        log.info("API CALL → Save Role | role={} roleCode={}",
                roleMaster.getRole(), roleMaster.getRoleCode());

        roleMaster.setCreatedOn(Helper.getCurrentTimeStamp());
        roleMaster.setUpdatedOn(Helper.getCurrentTimeStamp());

        try {
            RoleMaster savedRole = roleMasterRepository.save(roleMaster);

            log.info("SUCCESS → Role Saved | id={} role={} roleCode={}",
                    savedRole.getId(), savedRole.getRole(), savedRole.getRoleCode());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Roles",
                    "Create",
                    "Success",
                    savedRole.getId(),
                    savedRole.getRole(),
                    savedRole.getId(),
                    Map.of("role", savedRole.getRole(), "roleCode", savedRole.getRoleCode()),
                    request
            );

            return savedRole;
        } catch (Exception ex) {

            log.info("FAILED → Save Role | role={} reason={}",
                    roleMaster.getRole(), ex.getMessage());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Roles",
                    "Create",
                    "Failure",
                    null,
                    roleMaster.getRole(),
                    null,
                    Map.of("error", ex.getMessage()),
                    request
            );

            throw ex;
        }
    }

    // ======================= UPDATE =======================
    @Override
    public RoleMaster updateRoleMaster(RoleMaster roleMaster, Integer id, HttpServletRequest request) {

        log.info("API CALL → Update Role | id={}", id);

        Optional<RoleMaster> existingRoleMaster = roleMasterRepository.findById(id);

        if(existingRoleMaster.isPresent()){
            RoleMaster existing = existingRoleMaster.get();

            log.info("Updating Role | id={} oldRole={} newRole={} oldCode={} newCode={}",
                    id, existing.getRole(), roleMaster.getRole(),
                    existing.getRoleCode(), roleMaster.getRoleCode());

            Map<String, Object> previousData = Map.of(
                    "role", existing.getRole(),
                    "roleCode", existing.getRoleCode()
            );

            existing.setRole(roleMaster.getRole());
            existing.setRoleCode(roleMaster.getRoleCode());
            existing.setIsActive(roleMaster.getIsActive());
            existing.setUpdatedOn(Helper.getCurrentTimeStamp());

            RoleMaster updatedRole = roleMasterRepository.save(existing);

            log.info("SUCCESS → Role Updated | id={} role={} roleCode={}",
                    updatedRole.getId(), updatedRole.getRole(), updatedRole.getRoleCode());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Roles",
                    "Update",
                    "Success",
                    updatedRole.getId(),
                    updatedRole.getRole(),
                    updatedRole.getId(),
                    previousData,
                    request
            );

            return updatedRole;
        } else {

            log.info("FAILED → Update Role | id={} reason=Not Found", id);

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Roles",
                    "Update",
                    "Failure",
                    id,
                    null,
                    id,
                    Map.of("error", "RoleMaster not found"),
                    request
            );

            throw new ResourceNotFoundException("RoleMaster not found for ", "Id", id);
        }
    }

    // ======================= DELETE =======================
    @Override
    public void deleteByIdRoleMaster(Integer id) {

        log.info("API CALL → Delete Role | id={}", id);

        if (!roleMasterRepository.existsById(id)) {
            log.info("FAILED → Delete Role | id={} reason=Not Found", id);
            throw new ResourceNotFoundException("RoleMaster not found for", "Id", id);
        }

        roleMasterRepository.deleteById(id);

        log.info("SUCCESS → Role Deleted | id={}", id);
    }

    // ======================= FIND ALL =======================
    @Override
    public List<RoleMaster> findAllRoleMaster() {

        log.info("API CALL → Get All Roles");

        return roleMasterRepository.findAllRoleMasterOrdered();
    }

    // ======================= FIND BY ID =======================
    @Override
    public Optional<RoleMaster> findRoleMasterById(Integer id) {

        log.info("API CALL → Get Role By ID | id={}", id);

        return roleMasterRepository.findById(id);
    }

    // ======================= FIND ACTIVE =======================
    @Override
    public List<RoleMaster> findAllActiveRoleMaster(boolean isActive) {

        log.info("API CALL → Get Active Roles | isActive={}", isActive);

        return roleMasterRepository.findByIsActive(isActive);
    }

    // ======================= FIND BY NAME =======================
    @Override
    public RoleMaster findRoleByName(String name) {

        log.info("API CALL → Get Role By Name | name={}", name);

        Optional<RoleMaster> roleOptional = roleMasterRepository.findByRole(name);

        if (roleOptional.isPresent()) {
            RoleMaster role = roleOptional.get();
            log.info("SUCCESS → Role Found | id={} name={}", role.getId(), role.getRole());
            return role;
        } else {
            log.info("FAILED → Get Role | name={} reason=Not Found", name);
            throw new ResourceNotFoundException("Role", "name", name);
        }
    }

    // ======================= STATUS UPDATE =======================
    @Override
    public RoleMaster updateStatus(Integer id, boolean isActive, HttpServletRequest request) {

        log.info("API CALL → Role Status Update | id={} newStatus={}", id, isActive);

        RoleMaster roleMaster = roleMasterRepository.findById(id)
                .orElseThrow(() -> {

                    log.info("FAILED → Status Update | id={} reason=Not Found", id);

                    auditLogUtil.logAction(
                            currentUser.getCurrentEmployeeOrThrow(),
                            "Roles",
                            "StatusUpdate",
                            "Failure",
                            id,
                            null,
                            id,
                            Map.of("error", "RoleMaster not found"),
                            request
                    );

                    return new ResourceNotFoundException("Role", "id", id);
                });

        boolean oldStatus = roleMaster.getIsActive();

        roleMaster.setIsActive(isActive);
        roleMaster.setUpdatedOn(Helper.getCurrentTimeStamp());

        RoleMaster updatedRole = roleMasterRepository.save(roleMaster);

        log.info("SUCCESS → Role Status Updated | id={} oldStatus={} newStatus={} role={}",
                id, oldStatus, isActive, updatedRole.getRole());

        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),
                "Roles",
                "StatusUpdate",
                "Success",
                updatedRole.getId(),
                updatedRole.getRole(),
                updatedRole.getId(),
                Map.of("isActive", oldStatus),
                request
        );

        return updatedRole;
    }
}