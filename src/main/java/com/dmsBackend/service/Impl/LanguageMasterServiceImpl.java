package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.LanguageMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.Helper;
import com.dmsBackend.repository.LanguageMasterRepository;
import com.dmsBackend.response.LanguageMasterRequest;
import com.dmsBackend.service.LanguageMasterService;
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
public class LanguageMasterServiceImpl implements LanguageMasterService {

    @Autowired
    private LanguageMasterRepository languageMasterRepository;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private AuditLogUtil auditLogUtil;

    // ======================= CREATE =======================
    @Override
    public LanguageMaster saveLanguageMaster(LanguageMasterRequest request,
                                             HttpServletRequest httpRequest) {

        log.info("API CALL → Save Language | code={} name={}", request.getCode(), request.getName());

        // Duplicate check
        Optional<LanguageMaster> existing =
                languageMasterRepository.findByCodeAndIsActiveTrue(request.getCode());

        if (existing.isPresent()) {
            log.info("FAILED → Save Language | code={} reason=Duplicate code", request.getCode());
            throw new IllegalArgumentException("Language code already exists");
        }

        LanguageMaster languageMaster = new LanguageMaster();
        languageMaster.setCode(request.getCode());
        languageMaster.setName(request.getName());
        languageMaster.setIsActive(true);
        languageMaster.setCreatedOn(Helper.getCurrentTimeStamp());
        languageMaster.setUpdatedOn(Helper.getCurrentTimeStamp());

        try {
            LanguageMaster saved = languageMasterRepository.save(languageMaster);

            log.info("SUCCESS → Language Saved | id={} code={} name={}",
                    saved.getId(), saved.getCode(), saved.getName());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "LanguageMaster",
                    "Create",
                    "Success",
                    saved.getId().intValue(),
                    saved.getName(),
                    saved.getId().intValue(),
                    Map.of(
                            "code", saved.getCode(),
                            "name", saved.getName()
                    ),
                    httpRequest
            );

            return saved;

        } catch (Exception ex) {

            log.info("FAILED → Save Language | code={} name={} reason={}",
                    request.getCode(), request.getName(), ex.getMessage());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "LanguageMaster",
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
    public LanguageMaster updateLanguageMaster(LanguageMasterRequest request,
                                               Long id,
                                               HttpServletRequest httpRequest) {

        log.info("API CALL → Update Language | id={}", id);

        LanguageMaster existing = languageMasterRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("FAILED → Update Language | id={} reason=Not Found", id);
                    return new ResourceNotFoundException("LanguageMaster", "id", id);
                });

        // Duplicate check if code changed
        if (!existing.getCode().equals(request.getCode())) {
            Optional<LanguageMaster> duplicate =
                    languageMasterRepository.findByCodeAndIsActiveTrue(request.getCode());
            if (duplicate.isPresent()) {
                log.info("FAILED → Update Language | id={} reason=Duplicate code: {}",
                        id, request.getCode());
                throw new IllegalArgumentException("Language code already exists");
            }
        }

        log.info("Updating Language | id={} oldCode={} newCode={} oldName={} newName={}",
                id, existing.getCode(), request.getCode(), existing.getName(), request.getName());

        Map<String, Object> previousData = Map.of(
                "code", existing.getCode(),
                "name", existing.getName()
        );

        existing.setCode(request.getCode());
        existing.setName(request.getName());
        existing.setUpdatedOn(Helper.getCurrentTimeStamp());

        LanguageMaster updated = languageMasterRepository.save(existing);

        log.info("SUCCESS → Language Updated | id={} code={} name={}",
                updated.getId(), updated.getCode(), updated.getName());

        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),
                "LanguageMaster",
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
    public List<LanguageMaster> findAllLanguageMaster(int flag) {

        log.info("API CALL → Get All Languages | flag={} ({})",
                flag, flag == 1 ? "Active Only" : "All");

        List<LanguageMaster> languages = flag == 1
                ? languageMasterRepository.findByIsActiveTrue()
                : languageMasterRepository.findAll();

        log.info("SUCCESS → Retrieved {} Languages", languages.size());

        return languages;
    }

    // ======================= GET BY ID =======================
    @Override
    public LanguageMaster findLanguageMasterById(Long id) {

        log.info("API CALL → Get Language By ID | id={}", id);

        return languageMasterRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("FAILED → Get Language | id={} reason=Not Found", id);
                    return new ResourceNotFoundException("LanguageMaster", "id", id);
                });
    }

    // ======================= STATUS UPDATE =======================
    @Override
    public LanguageMaster updateStatus(Long id,
                                       Boolean isActive,
                                       HttpServletRequest request) {

        log.info("API CALL → Language Status Update | id={} newStatus={}", id, isActive);

        LanguageMaster languageMaster = languageMasterRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("FAILED → Status Update | id={} reason=Not Found", id);
                    return new ResourceNotFoundException("LanguageMaster", "id", id);
                });

        Boolean oldStatus = languageMaster.getIsActive();

        languageMaster.setIsActive(isActive);
        languageMaster.setUpdatedOn(Helper.getCurrentTimeStamp());

        LanguageMaster updated = languageMasterRepository.save(languageMaster);

        log.info("SUCCESS → Language Status Updated | id={} oldStatus={} newStatus={} name={}",
                id, oldStatus, isActive, updated.getName());

        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),
                "LanguageMaster",
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