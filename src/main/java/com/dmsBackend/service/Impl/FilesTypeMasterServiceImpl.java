package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.FilesTypeMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.Helper;
import com.dmsBackend.repository.EmployeeRepository;
import com.dmsBackend.repository.FilesTypeMasterRepository;
import com.dmsBackend.response.ApiResponse;
import com.dmsBackend.service.FilesTypeMasterService;
import com.dmsBackend.utils.AuditLogUtil;
import com.dmsBackend.utils.CurrentUser;
import com.dmsBackend.utils.ResponseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class FilesTypeMasterServiceImpl implements FilesTypeMasterService {

    @Autowired
    private FilesTypeMasterRepository filesTypeMasterRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    CurrentUser currentUser;

    @Autowired
    AuditLogUtil auditLogUtil;

    // ======================= FIND ALL =======================
    @Override
    public ApiResponse<List<FilesTypeMaster>> getAllFilesTypeMaster() {

        log.info("API CALL → Get All File Types");

        List<FilesTypeMaster> filesTypeMasters = filesTypeMasterRepository.findAllFilesTypeMasterOrdered();

        if (filesTypeMasters.isEmpty()) {
            log.info("FAILED → Get All File Types | reason=No Records Found");
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "RECORD NOT FOUND", 400);
        }

        log.info("SUCCESS → Retrieved {} File Types", filesTypeMasters.size());

        return ResponseUtils.createSuccessResponse(filesTypeMasters, new TypeReference<>() {});
    }

    // ======================= FIND ALL ACTIVE =======================
    @Override
    public ApiResponse<List<FilesTypeMaster>> getAllActiveFilesTypeMaster() {

        log.info("API CALL → Get All Active File Types");

        List<FilesTypeMaster> filesTypeMasters = filesTypeMasterRepository.findActiveFileType();

        if (filesTypeMasters.isEmpty()) {
            log.info("FAILED → Get Active File Types | reason=No Records Found");
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "RECORD NOT FOUND", 400);
        }

        log.info("SUCCESS → Retrieved {} Active File Types", filesTypeMasters.size());

        return ResponseUtils.createSuccessResponse(filesTypeMasters, new TypeReference<>() {});
    }

    // ======================= FIND BY ID =======================
    @Override
    public ApiResponse<FilesTypeMaster> getFilesTypeMasterById(Integer id) {

        log.info("API CALL → Get File Type By ID | id={}", id);

        if (id == null) {
            log.info("FAILED → Get File Type | reason=ID is null");
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "ID cannot be null", 400);
        }

        Optional<FilesTypeMaster> fileType = filesTypeMasterRepository.findById(id);

        if (fileType.isPresent()) {
            log.info("SUCCESS → File Type Found | id={} name={}", id, fileType.get().getFiletype());
            return ResponseUtils.createSuccessResponse(fileType.get(), new TypeReference<>() {});
        } else {
            log.info("FAILED → Get File Type | id={} reason=Not Found", id);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "fileType not found", 404);
        }
    }

    // ======================= CREATE =======================
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public ApiResponse<FilesTypeMaster> createFilesTypeMaster(FilesTypeMaster filesTypeMaster, HttpServletRequest request) {

        log.info("API CALL → Create File Type | name={} extension={}",
                filesTypeMaster.getFiletype(), filesTypeMaster.getExtension());

        try {
            if (filesTypeMaster == null) {
                log.info("FAILED → Create File Type | reason=Object is null");
                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                        "filesTypeMaster object cannot be null", HttpStatus.BAD_REQUEST.value());
            }

            if (filesTypeMaster.getExtension() == null || filesTypeMaster.getExtension().isEmpty()) {
                log.info("FAILED → Create File Type | reason=Extension is blank");
                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                        "filesTypeMaster CODE CANNOT BE BLANK", HttpStatus.BAD_REQUEST.value());
            }

            if (filesTypeMaster.getFiletype() == null || filesTypeMaster.getFiletype().isEmpty()) {
                log.info("FAILED → Create File Type | reason=Name is blank");
                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                        "filesTypeMaster CANNOT BE BLANK", HttpStatus.BAD_REQUEST.value());
            }

            boolean exists = filesTypeMasterRepository.existsByExtension(filesTypeMaster.getExtension());
            if (exists) {

                log.info("FAILED → Create File Type | extension={} reason=Duplicate Entry",
                        filesTypeMaster.getExtension());

                auditLogUtil.logAction(
                        currentUser.getCurrentEmployeeOrThrow(),
                        "FileType",
                        "Create",
                        "Failure",
                        null,
                        filesTypeMaster.getFiletype(),
                        null,
                        Map.of("error", "Duplicate entry for extension: " + filesTypeMaster.getExtension()),
                        request
                );

                return ResponseUtils.createFailureResponse(null, new TypeReference<FilesTypeMaster>() {},
                        "File type with extension '" + filesTypeMaster.getExtension() + "' already exists",
                        HttpStatus.CONFLICT.value());
            }

            filesTypeMaster.setCreatedOn(Helper.getCurrentTimeStamp());
            filesTypeMaster.setUpdatedOn(Helper.getCurrentTimeStamp());
            filesTypeMaster.setIsActive(1);

            FilesTypeMaster savedFileType = filesTypeMasterRepository.save(filesTypeMaster);

            log.info("SUCCESS → File Type Created | id={} name={} extension={}",
                    savedFileType.getId(), savedFileType.getFiletype(), savedFileType.getExtension());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "FileType",
                    "Create",
                    "Success",
                    savedFileType.getId(),
                    savedFileType.getFiletype(),
                    savedFileType.getId(),
                    Map.of("name", savedFileType.getFiletype(), "extension", savedFileType.getExtension()),
                    request
            );

            return ResponseUtils.createSuccessResponse(savedFileType, new TypeReference<FilesTypeMaster>() {});

        } catch (ConstraintViolationException e) {

            log.info("FAILED → Create File Type | name={} reason=Validation Error: {}",
                    filesTypeMaster.getFiletype(), e.getMessage());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "FileType",
                    "Create",
                    "Failure",
                    null,
                    filesTypeMaster.getFiletype(),
                    null,
                    Map.of("error", e.getMessage()),
                    request
            );

            return ResponseUtils.createFailureResponse(null, new TypeReference<FilesTypeMaster>() {},
                    "Validation failed for required fields: " + e.getMessage(), HttpStatus.BAD_REQUEST.value());
        } catch (Exception e) {

            log.info("FAILED → Create File Type | name={} reason={}",
                    filesTypeMaster != null ? filesTypeMaster.getFiletype() : "null", e.getMessage());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "FileType",
                    "Create",
                    "Failure",
                    null,
                    filesTypeMaster != null ? filesTypeMaster.getFiletype() : null,
                    null,
                    Map.of("error", e.getMessage()),
                    request
            );

            return ResponseUtils.createFailureResponse(null, new TypeReference<FilesTypeMaster>() {},
                    "Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // ======================= UPDATE =======================
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public ApiResponse<FilesTypeMaster> updateFilesTypeMaster(Integer id, FilesTypeMaster updatedFilesTypeMaster, HttpServletRequest request) {

        log.info("API CALL → Update File Type | id={}", id);

        try {
            if (updatedFilesTypeMaster == null) {
                log.info("FAILED → Update File Type | id={} reason=Object is null", id);
                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                        "Updated object cannot be null", 400);
            }

            Optional<FilesTypeMaster> existExtension = filesTypeMasterRepository.findByExtension(updatedFilesTypeMaster.getExtension());

            if (existExtension.isPresent() && !existExtension.get().getId().equals(id)) {
                log.info("FAILED → Update File Type | id={} reason=Extension already exists", id);
                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                        "Extension already exists", 400);
            }

            Optional<FilesTypeMaster> existingFileTypeOpt = filesTypeMasterRepository.findById(id);

            if (existingFileTypeOpt.isPresent()) {
                FilesTypeMaster existingFileType = existingFileTypeOpt.get();

                log.info("Updating File Type | id={} oldName={} newName={} oldExtension={} newExtension={}",
                        id, existingFileType.getFiletype(), updatedFilesTypeMaster.getFiletype(),
                        existingFileType.getExtension(), updatedFilesTypeMaster.getExtension());

                Map<String, Object> previousData = Map.of(
                        "name", existingFileType.getFiletype(),
                        "extension", existingFileType.getExtension()
                );

                existingFileType.setFiletype(updatedFilesTypeMaster.getFiletype());
                existingFileType.setExtension(updatedFilesTypeMaster.getExtension());
                existingFileType.setIsActive(updatedFilesTypeMaster.getIsActive());
                existingFileType.setUpdatedOn(Helper.getCurrentTimeStamp());

                FilesTypeMaster savedFileType = filesTypeMasterRepository.save(existingFileType);

                log.info("SUCCESS → File Type Updated | id={} name={} extension={}",
                        savedFileType.getId(), savedFileType.getFiletype(), savedFileType.getExtension());

                auditLogUtil.logAction(
                        currentUser.getCurrentEmployeeOrThrow(),
                        "FileType",
                        "Update",
                        "Success",
                        savedFileType.getId(),
                        savedFileType.getFiletype(),
                        savedFileType.getId(),
                        previousData,
                        request
                );

                return ResponseUtils.createSuccessResponse(savedFileType, new TypeReference<FilesTypeMaster>() {});
            } else {

                log.info("FAILED → Update File Type | id={} reason=Not Found", id);

                auditLogUtil.logAction(
                        currentUser.getCurrentEmployeeOrThrow(),
                        "FileType",
                        "Update",
                        "Failure",
                        id,
                        null,
                        id,
                        Map.of("error", "FileType not found"),
                        request
                );

                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                        "File Type Master not found", 404);
            }
        } catch (ConstraintViolationException e) {

            log.info("FAILED → Update File Type | id={} reason=Validation Error: {}", id, e.getMessage());

            return ResponseUtils.createFailureResponse(null, new TypeReference<FilesTypeMaster>() {},
                    "Validation failed for required fields: " + e.getMessage(), HttpStatus.BAD_REQUEST.value());
        }
    }

    // ======================= STATUS UPDATE =======================
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public ApiResponse<FilesTypeMaster> updateFileTypeStatus(Integer fileTypeId, Integer status, HttpServletRequest request) {

        log.info("API CALL → File Type Status Update | id={} newStatus={}", fileTypeId, status);

        if (fileTypeId == null) {
            log.info("FAILED → Status Update | reason=ID is null");
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                    "FileType ID cannot be blank", 400);
        }

        if (status == null || (!status.equals(1) && !status.equals(0))) {
            log.info("FAILED → Status Update | id={} reason=Invalid Status Value: {}", fileTypeId, status);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                    "Status must be either '1' (Active) or '0' (Inactive)", 400);
        }

        FilesTypeMaster filesTypeMaster = filesTypeMasterRepository.findById(fileTypeId)
                .orElseThrow(() -> {

                    log.info("FAILED → Status Update | id={} reason=Not Found", fileTypeId);

                    auditLogUtil.logAction(
                            currentUser.getCurrentEmployeeOrThrow(),
                            "FileType",
                            "StatusUpdate",
                            "Failure",
                            fileTypeId,
                            null,
                            fileTypeId,
                            Map.of("error", "FileType not found"),
                            request
                    );

                    return new ResourceNotFoundException("FileType", "id", fileTypeId);
                });

        Integer oldStatus = filesTypeMaster.getIsActive();

        filesTypeMaster.setIsActive(status);
        filesTypeMaster.setUpdatedOn(Helper.getCurrentTimeStamp());

        FilesTypeMaster updatedFileType = filesTypeMasterRepository.save(filesTypeMaster);

        log.info("SUCCESS → File Type Status Updated | id={} oldStatus={} newStatus={} name={}",
                fileTypeId, oldStatus, status, updatedFileType.getFiletype());

        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),
                "FileType",
                "StatusUpdate",
                "Success",
                updatedFileType.getId(),
                updatedFileType.getFiletype(),
                updatedFileType.getId(),
                Map.of("isActive", oldStatus),
                request
        );

        return ResponseUtils.createSuccessResponse(updatedFileType, new TypeReference<FilesTypeMaster>() {});
    }
}