package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.BaseMasterEntity;
import com.dmsBackend.entity.CodedMaster;
import com.dmsBackend.entity.ParentAware;
import com.dmsBackend.entity.ParentedMasterEntity;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.Helper;
import com.dmsBackend.repository.MasterRepository;
import com.dmsBackend.repository.ParentedMasterRepository;
import com.dmsBackend.response.MasterRequest;
import com.dmsBackend.service.MasterService;
import com.dmsBackend.utils.AuditLogUtil;
import com.dmsBackend.utils.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * All CRUD + audit-log logic for every master type lives here, ONCE.
 * Each concrete master (CaseType, CrimeType, State, District, City,
 * Priority, EvidenceType, ForwardingAuthorityType, ModeOfSubmission,
 * PackageType) only needs a 15-line subclass that wires up its own
 * repository (see CaseTypeMasterServiceImpl etc.).
 */
@Slf4j
public abstract class GenericMasterServiceImpl<T extends BaseMasterEntity> implements MasterService<T> {

    @Autowired
    protected CurrentUser currentUser;

    @Autowired
    protected AuditLogUtil auditLogUtil;

    /** The Spring Data repository backing this master type. */
    protected abstract MasterRepository<T> getRepository();

    /** Short label used in logs / audit trail, e.g. "CaseType". */
    protected abstract String getEntityLabel();

    /** A fresh, empty instance of T. */
    protected abstract T newInstance();

    // ======================= CREATE =======================
    @Override
    public T save(MasterRequest request, HttpServletRequest httpRequest) {

        log.info("API CALL → Save {} | name={}", getEntityLabel(), request.getName());

        Optional<T> existing = getRepository().findByNameAndIsActiveTrue(request.getName());
        if (existing.isPresent()) {
            log.info("FAILED → Save {} | name={} reason=Duplicate name", getEntityLabel(), request.getName());
            throw new IllegalArgumentException(getEntityLabel() + " name already exists");
        }

        T entity = newInstance();
        entity.setName(request.getName());
        entity.setIsActive(true);
        entity.setCreatedOn(Helper.getCurrentTimeStamp());
        entity.setUpdatedOn(Helper.getCurrentTimeStamp());

        if (entity instanceof ParentAware && request.getParentId() != null) {
            ((ParentAware) entity).setParentId(request.getParentId());
        }

        if (entity instanceof CodedMaster && request.getCode() != null) {
            ((CodedMaster) entity).setCode(request.getCode());
        }

        try {
            T saved = getRepository().save(entity);

            log.info("SUCCESS → {} Saved | id={} name={}", getEntityLabel(), saved.getId(), saved.getName());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    getEntityLabel() + "Master",
                    "Create",
                    "Success",
                    saved.getId().intValue(),
                    saved.getName(),
                    saved.getId().intValue(),
                    Map.of("name", saved.getName()),
                    httpRequest
            );

            return saved;

        } catch (Exception ex) {

            log.info("FAILED → Save {} | name={} reason={}", getEntityLabel(), request.getName(), ex.getMessage());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    getEntityLabel() + "Master",
                    "Create",
                    "Failure",
                    null,
                    request.getName(),
                    null,
                    Map.of("error", ex.getMessage()),
                    httpRequest
            );

            throw ex;
        }
    }

    // ======================= UPDATE =======================
    @Override
    public T update(MasterRequest request, Integer id, HttpServletRequest httpRequest) {

        log.info("API CALL → Update {} | id={}", getEntityLabel(), id);

        T existing = getRepository().findById(id)
                .orElseThrow(() -> {
                    log.info("FAILED → Update {} | id={} reason=Not Found", getEntityLabel(), id);
                    return new ResourceNotFoundException(getEntityLabel() + "Master", "id", id);
                });

        if (!existing.getName().equals(request.getName())) {
            Optional<T> duplicate = getRepository().findByNameAndIsActiveTrue(request.getName());
            if (duplicate.isPresent()) {
                log.info("FAILED → Update {} | id={} reason=Duplicate name: {}",
                        getEntityLabel(), id, request.getName());
                throw new IllegalArgumentException(getEntityLabel() + " name already exists");
            }
        }

        log.info("Updating {} | id={} oldName={} newName={}",
                getEntityLabel(), id, existing.getName(), request.getName());

        Map<String, Object> previousData = Map.of("name", existing.getName());

        existing.setName(request.getName());
        existing.setUpdatedOn(Helper.getCurrentTimeStamp());

        if (existing instanceof ParentAware && request.getParentId() != null) {
            ((ParentAware) existing).setParentId(request.getParentId());
        }

        if (existing instanceof CodedMaster && request.getCode() != null) {
            ((CodedMaster) existing).setCode(request.getCode());
        }

        T updated = getRepository().save(existing);

        log.info("SUCCESS → {} Updated | id={} name={}", getEntityLabel(), updated.getId(), updated.getName());

        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),
                getEntityLabel() + "Master",
                "Update",
                "Success",
                updated.getId().intValue(),
                updated.getName(),
                updated.getId().intValue(),
                previousData,
                httpRequest
        );

        return updated;
    }

    // ======================= GET ALL =======================
    @Override
    public List<T> findAll(int flag) {

        log.info("API CALL → Get All {} | flag={} ({})",
                getEntityLabel(), flag, flag == 1 ? "Active Only" : "All");

        List<T> list = flag == 1
                ? getRepository().findByIsActiveTrue()
                : getRepository().findAll();

        log.info("SUCCESS → Retrieved {} {} records", list.size(), getEntityLabel());

        return list;
    }

    // ======================= GET BY PARENT =======================
    @Override
    @SuppressWarnings("unchecked")
    public List<T> findByParent(Integer parentId, int flag) {

        log.info("API CALL → Get {} By Parent | parentId={} flag={}", getEntityLabel(), parentId, flag);

        MasterRepository<T> repository = getRepository();
        if (!(repository instanceof ParentedMasterRepository)) {
            throw new UnsupportedOperationException(getEntityLabel() + " does not support parent-based lookup");
        }

        ParentedMasterRepository<? extends ParentedMasterEntity> parentedRepository =
                (ParentedMasterRepository<? extends ParentedMasterEntity>) repository;

        List<T> list = flag == 1
                ? (List<T>) parentedRepository.findByParentIdAndIsActiveTrue(parentId)
                : (List<T>) parentedRepository.findByParentId(parentId);

        log.info("SUCCESS → Retrieved {} {} records for parentId={}", list.size(), getEntityLabel(), parentId);

        return list;
    }

    // ======================= GET BY ID =======================
    @Override
    public T findById(Integer id) {

        log.info("API CALL → Get {} By ID | id={}", getEntityLabel(), id);

        return getRepository().findById(id)
                .orElseThrow(() -> {
                    log.info("FAILED → Get {} | id={} reason=Not Found", getEntityLabel(), id);
                    return new ResourceNotFoundException(getEntityLabel() + "Master", "id", id);
                });
    }

    // ======================= STATUS UPDATE =======================
    @Override
    public T updateStatus(Integer id, Boolean isActive, HttpServletRequest request) {

        log.info("API CALL → {} Status Update | id={} newStatus={}", getEntityLabel(), id, isActive);

        T entity = getRepository().findById(id)
                .orElseThrow(() -> {
                    log.info("FAILED → Status Update | id={} reason=Not Found", id);
                    return new ResourceNotFoundException(getEntityLabel() + "Master", "id", id);
                });

        Boolean oldStatus = entity.getIsActive();

        entity.setIsActive(isActive);
        entity.setUpdatedOn(Helper.getCurrentTimeStamp());

        T updated = getRepository().save(entity);

        log.info("SUCCESS → {} Status Updated | id={} oldStatus={} newStatus={} name={}",
                getEntityLabel(), id, oldStatus, isActive, updated.getName());

        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),
                getEntityLabel() + "Master",
                "StatusUpdate",
                "Success",
                updated.getId().intValue(),
                updated.getName(),
                updated.getId().intValue(),
                Map.of("isActive", oldStatus),
                request
        );

        return updated;
    }
}