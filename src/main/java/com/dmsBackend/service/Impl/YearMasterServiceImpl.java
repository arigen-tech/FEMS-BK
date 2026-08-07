package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.YearMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.Helper;
import com.dmsBackend.repository.YearMasterRepository;
import com.dmsBackend.service.YearMasterService;
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
public class YearMasterServiceImpl implements YearMasterService {

    @Autowired
    YearMasterRepository yearMasterRepository;

    @Autowired
    CurrentUser currentUser;

    @Autowired
    AuditLogUtil auditLogUtil;

    // ======================= SAVE =======================
    @Override
    public YearMaster saveYearMaster(YearMaster yearMaster, HttpServletRequest request) {

        log.info("API CALL → Save Year | name={}", yearMaster.getName());

        if (!yearMaster.getName().startsWith("20")) {
            log.info("FAILED → Save Year | name={} reason=Must start with '20'", yearMaster.getName());
            throw new IllegalArgumentException("Year must start with '20'");
        }

        int lastTwoDigits = Integer.parseInt(yearMaster.getName().substring(yearMaster.getName().length() - 2));
        if (lastTwoDigits > 50) {
            log.info("FAILED → Save Year | name={} reason=Must end with number ≤ 50", yearMaster.getName());
            throw new IllegalArgumentException("Year must end with a number less than or equal to 50");
        }

        if (yearMasterRepository.existsByName(yearMaster.getName())) {
            log.info("FAILED → Save Year | name={} reason=Year already exists", yearMaster.getName());
            throw new IllegalArgumentException("Year name already exists");
        }

        yearMaster.setIsActive(1);
        yearMaster.setCreatedOn(Helper.getCurrentTimeStamp());
        yearMaster.setUpdatedOn(Helper.getCurrentTimeStamp());

        try {
            YearMaster savedYear = yearMasterRepository.save(yearMaster);

            log.info("SUCCESS → Year Saved | id={} name={}",
                    savedYear.getId(), savedYear.getName());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Years",
                    "Create",
                    "Success",
                    savedYear.getId(),
                    savedYear.getName(),
                    savedYear.getId(),
                    Map.of("name", savedYear.getName()),
                    request
            );

            return savedYear;
        } catch (Exception ex) {

            log.info("FAILED → Save Year | name={} reason={}",
                    yearMaster.getName(), ex.getMessage());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Years",
                    "Create",
                    "Failure",
                    null,
                    yearMaster.getName(),
                    null,
                    Map.of("error", ex.getMessage()),
                    request
            );

            throw ex;
        }
    }

    // ======================= UPDATE =======================
    @Override
    public YearMaster updateYearMaster(YearMaster yearMaster, Integer id, HttpServletRequest request) {

        log.info("API CALL → Update Year | id={}", id);

        Optional<YearMaster> yearMasterOptional = yearMasterRepository.findById(id);

        if (yearMasterOptional.isPresent()) {
            YearMaster existing = yearMasterOptional.get();

            log.info("Updating Year | id={} oldName={} newName={}",
                    id, existing.getName(), yearMaster.getName());

            if (!yearMaster.getName().startsWith("20")) {
                log.info("FAILED → Update Year | id={} reason=Must start with '20'", id);
                throw new IllegalArgumentException("Year name must start with '20'");
            }

            if (!existing.getName().equals(yearMaster.getName())
                    && yearMasterRepository.existsByName(yearMaster.getName())) {
                log.info("FAILED → Update Year | id={} reason=Year already exists", id);
                throw new IllegalArgumentException("Year name already exists");
            }

            Map<String, Object> previousData = Map.of(
                    "name", existing.getName()
            );

            existing.setName(yearMaster.getName());
            existing.setUpdatedOn(Helper.getCurrentTimeStamp());

            YearMaster updatedYear = yearMasterRepository.save(existing);

            log.info("SUCCESS → Year Updated | id={} name={}",
                    updatedYear.getId(), updatedYear.getName());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "year",
                    "Update",
                    "Success",
                    updatedYear.getId(),
                    updatedYear.getName(),
                    updatedYear.getId(),
                    previousData,
                    request
            );

            return updatedYear;

        } else {

            log.info("FAILED → Update Year | id={} reason=Not Found", id);

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "year",
                    "Update",
                    "Failure",
                    id,
                    null,
                    id,
                    Map.of("error", "YearMaster not found"),
                    request
            );

            throw new ResourceNotFoundException("YearMaster not found for", "Id", id);
        }
    }

    // ======================= DELETE =======================
    @Override
    public void deleteByIdYearMaster(Integer id) {

        log.info("API CALL → Delete Year | id={}", id);

        this.yearMasterRepository.deleteById(id);

        log.info("SUCCESS → Year Deleted | id={}", id);
    }

    // ======================= FIND ALL =======================
    @Override
    public List<YearMaster> findAllYearMaster() {

        log.info("API CALL → Get All Years");

        return yearMasterRepository.findAllYearMasterOrdered();
    }

    // ======================= FIND ACTIVE =======================
    @Override
    public List<YearMaster> findAllActiveYearMaster(int isActive) {

        log.info("API CALL → Get Active Years | isActive={}", isActive);

        return yearMasterRepository.findByIsActive(isActive);
    }

    // ======================= FIND BY ID =======================
    @Override
    public Optional<YearMaster> findYearMasterById(Integer id) {

        log.info("API CALL → Get Year By ID | id={}", id);

        return yearMasterRepository.findById(id);
    }

    @Override
    public YearMaster findByIdyear(Integer id) {

        log.info("API CALL → Get Year (Strict) | id={}", id);

        return yearMasterRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("FAILED → Get Year | id={} reason=Not Found", id);
                    return new ResourceNotFoundException("year not found", "Id", id);
                });
    }

    // ======================= STATUS UPDATE =======================
    @Override
    public YearMaster updateStatus(Integer id, Integer isActive, HttpServletRequest request) {

        log.info("API CALL → Year Status Update | id={} newStatus={}", id, isActive);

        YearMaster yearMaster = yearMasterRepository.findById(id)
                .orElseThrow(() -> {

                    log.info("FAILED → Status Update | id={} reason=Not Found", id);

                    auditLogUtil.logAction(
                            currentUser.getCurrentEmployeeOrThrow(),
                            "year",
                            "StatusUpdate",
                            "Failure",
                            id,
                            null,
                            id,
                            Map.of("error", "yearMaster not found"),
                            request
                    );

                    return new ResourceNotFoundException("yearMaster", "id", id);
                });

        Integer oldStatus = yearMaster.getIsActive();

        yearMaster.setUpdatedOn(Helper.getCurrentTimeStamp());
        yearMaster.setIsActive(isActive);

        YearMaster updatedYear = yearMasterRepository.save(yearMaster);

        log.info("SUCCESS → Year Status Updated | id={} oldStatus={} newStatus={}",
                id, oldStatus, isActive);

        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),
                "year",
                "StatusUpdate",
                "Success",
                updatedYear.getId(),
                updatedYear.getName(),
                updatedYear.getId(),
                Map.of("isActive", oldStatus),
                request
        );

        return updatedYear;
    }
}