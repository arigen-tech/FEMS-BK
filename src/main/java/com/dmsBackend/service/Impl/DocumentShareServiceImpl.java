package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.*;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.repository.DocumentDetailsRepository;
import com.dmsBackend.repository.DocumentHeaderRepository;
import com.dmsBackend.repository.DocumentShareRepository;
import com.dmsBackend.repository.EmployeeRepository;
import com.dmsBackend.response.*;
import com.dmsBackend.service.DocumentShareService;
import com.dmsBackend.service.NotificationService;
import com.dmsBackend.utils.AuditLogUtil;
import com.dmsBackend.utils.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentShareServiceImpl implements DocumentShareService {

    @Autowired
    private DocumentShareRepository documentShareRepository;

    @Autowired
    private DocumentDetailsRepository documentDetailsRepository;

    @Autowired
    private DocumentHeaderRepository documentHeaderRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogUtil auditLogUtil;

    // ======================= SHARE DOCUMENT =======================
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW, timeout = 30)
    public ApiResponse<DocumentShareResponse> shareDocument(DocumentShareRequest request,
                                                            HttpServletRequest httpRequest) {

        log.info("API CALL → Share Document | headerId={} files={} recipients={}",
                request.getDocumentHeaderId(),
                request.getDocumentDetailIds() != null ? request.getDocumentDetailIds().size() : 0,
                request.getRecipientIds() != null ? request.getRecipientIds().size() : 0);

        ApiResponse<DocumentShareResponse> response = new ApiResponse<>();
        DocumentShareResponse shareResponse = new DocumentShareResponse();
        List<DocumentShareResponse.ShareResult> results = new ArrayList<>();

        try {
            // === VALIDATION PHASE ===
            if (request == null || request.getDocumentHeaderId() == null) {
                log.info("FAILED → Share Document | reason=Header ID is required");
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setMessage("Document Header ID is required");
                return response;
            }

            if (request.getDocumentDetailIds() == null || request.getDocumentDetailIds().isEmpty()) {
                log.info("FAILED → Share Document | headerId={} reason=No files specified", request.getDocumentHeaderId());
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setMessage("No document files specified to share");
                return response;
            }

            if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
                log.info("FAILED → Share Document | headerId={} reason=No recipients specified", request.getDocumentHeaderId());
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setMessage("No recipients specified");
                return response;
            }

            // Get current user
            Employee currentEmployee;
            try {
                currentEmployee = currentUser.getCurrentEmployeeOrThrow();
                log.debug("Current user: {} (ID: {})", currentEmployee.getName(), currentEmployee.getId());
            } catch (Exception e) {
                log.info("FAILED → Share Document | reason=User not authenticated");
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setMessage("User not authenticated");
                return response;
            }

            Integer currentDepartmentId = currentEmployee.getDepartment().getId();

            // Validate and get document header
            DocumentHeader documentHeader;
            try {
                documentHeader = documentHeaderRepository.findById(request.getDocumentHeaderId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Document header not found with id: " + request.getDocumentHeaderId()));
                log.debug("Found document header: {} (ID: {})", documentHeader.getTitle(), documentHeader.getId());
            } catch (ResourceNotFoundException e) {
                log.info("FAILED → Share Document | headerId={} reason=Header not found", request.getDocumentHeaderId());
                response.setStatus(HttpStatus.NOT_FOUND.value());
                response.setMessage(e.getMessage());
                return response;
            }

            // Get all specified document details
            List<DocumentDetails> documentsToShare = new ArrayList<>();
            for (Integer detailId : request.getDocumentDetailIds()) {
                try {
                    DocumentDetails document = documentDetailsRepository.findById(detailId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Document file not found with id: " + detailId));

                    // Validate document belongs to the specified header
                    if (!document.getDocumentHeader().getId().equals(documentHeader.getId())) {
                        log.warn("Document detail {} does not belong to header {}", detailId, documentHeader.getId());
                        continue;
                    }

                    // Check if document is approved
                    if (document.getStatus() != DocApprovalStatus.APPROVED) {
                        log.warn("Document {} is not approved, status: {}", detailId, document.getStatus());
                        continue;
                    }

                    // Check if document is in trash
                    if (Boolean.TRUE.equals(document.getIsDeleted())) {
                        log.warn("Document {} is in trash, cannot share", detailId);
                        continue;
                    }

                    documentsToShare.add(document);
                    log.debug("Added document for sharing: {} (ID: {})", document.getDocName(), document.getId());
                } catch (ResourceNotFoundException e) {
                    log.warn("Document detail {} not found: {}", detailId, e.getMessage());
                }
            }

            if (documentsToShare.isEmpty()) {
                log.info("FAILED → Share Document | headerId={} reason=No valid approved files found", request.getDocumentHeaderId());
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setMessage("No valid approved files found to share");
                return response;
            }

            log.info("Found {} valid approved files to share for document header {}",
                    documentsToShare.size(), documentHeader.getId());

            // === SHARING PHASE ===
            int totalSharesCreated = 0;

            for (Integer recipientId : request.getRecipientIds()) {
                DocumentShareResponse.ShareResult result = new DocumentShareResponse.ShareResult();

                try {
                    // Get recipient employee
                    Employee recipient = employeeRepository.findById(recipientId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Recipient not found with id: " + recipientId));

                    result.setEmployeeName(recipient.getName());

                    // Validate recipient
                    String validationError = validateRecipient(recipient, currentEmployee, currentDepartmentId);
                    if (validationError != null) {
                        result.setSuccess(false);
                        result.setMessage(validationError);
                        results.add(result);
                        log.debug("Recipient validation failed for {}: {}", recipient.getName(), validationError);
                        continue;
                    }

                    int filesSharedForThisRecipient = 0;
                    int filesAlreadySharedForThisRecipient = 0;

                    // Share each specified document
                    for (DocumentDetails document : documentsToShare) {
                        // Check if this specific file is already shared with this employee
                        if (isFileAlreadyShared(document.getId(), recipientId)) {
                            filesAlreadySharedForThisRecipient++;
                            log.debug("File {} already shared with recipient {}, skipping",
                                    document.getId(), recipientId);
                            continue;
                        }

                        // Create new share record for this file
                        DocumentShare share = createShare(
                                document,
                                documentHeader,
                                currentEmployee,
                                recipient,
                                request.getEndTime()
                        );

                        DocumentShare savedShare = documentShareRepository.save(share);
                        totalSharesCreated++;
                        filesSharedForThisRecipient++;

                        log.debug("Created share record ID: {} for file: {}, recipient: {}",
                                savedShare.getId(), document.getDocName(), recipient.getName());

                        // Send notification immediately (synchronous)
                        sendNotification(savedShare);

                        // Log audit trail asynchronously (non-blocking)
                        logAuditAsync(currentEmployee, documentHeader, document, recipient,
                                savedShare, request.getEndTime(), httpRequest);
                    }

                    if (filesSharedForThisRecipient > 0) {
                        result.setSuccess(true);
                        result.setMessage(String.format(
                                "Shared %d file(s) successfully. %d file(s) were already shared.",
                                filesSharedForThisRecipient,
                                filesAlreadySharedForThisRecipient
                        ));
                        result.setDocumentShareId((long) totalSharesCreated);
                        log.debug("Successfully shared {} files with recipient {}",
                                filesSharedForThisRecipient, recipient.getName());
                    } else if (filesAlreadySharedForThisRecipient > 0) {
                        result.setSuccess(false);
                        result.setMessage("All specified files were already shared with this employee");
                        log.debug("All files already shared with recipient {}", recipient.getName());
                    } else {
                        result.setSuccess(false);
                        result.setMessage("No files could be shared");
                    }

                    results.add(result);

                } catch (ResourceNotFoundException e) {
                    log.warn("Resource not found for recipient {}: {}", recipientId, e.getMessage());
                    result.setEmployeeName("Recipient ID: " + recipientId);
                    result.setSuccess(false);
                    result.setMessage(e.getMessage());
                    results.add(result);
                } catch (Exception e) {
                    log.error("Unexpected error sharing with recipient {}: {}", recipientId, e.getMessage(), e);
                    result.setEmployeeName("Recipient ID: " + recipientId);
                    result.setSuccess(false);
                    result.setMessage("Error: " + e.getMessage());
                    results.add(result);
                }
            }

            // === RESPONSE PHASE ===
            long successCount = results.stream()
                    .filter(DocumentShareResponse.ShareResult::isSuccess)
                    .count();
            long failureCount = results.size() - successCount;

            shareResponse.setSharedBy(currentEmployee.getName());
            shareResponse.setSharedTo(results.stream()
                    .filter(DocumentShareResponse.ShareResult::isSuccess)
                    .map(DocumentShareResponse.ShareResult::getEmployeeName)
                    .collect(Collectors.toList()));
            shareResponse.setDocumentName(documentHeader.getTitle());
            shareResponse.setCreatedAt(LocalDateTime.now());
            shareResponse.setEndTime(request.getEndTime());
            shareResponse.setResults(results);
            shareResponse.setTotalFilesShared(totalSharesCreated);
            shareResponse.setStatus(String.format(
                    "Created %d total share records. Successfully shared with %d employee(s), %d failed",
                    totalSharesCreated,
                    successCount,
                    failureCount
            ));

            // Set HTTP response status
            if (successCount > 0 || totalSharesCreated > 0) {
                response.setStatus(HttpStatus.OK.value());
                response.setMessage(String.format(
                        "Shared %d file(s) with %d recipient(s)",
                        totalSharesCreated,
                        successCount
                ));
                log.info("SUCCESS → Document Shared | headerId={} files={} recipients={} sharesCreated={}",
                        request.getDocumentHeaderId(), documentsToShare.size(), successCount, totalSharesCreated);
            } else {
                log.info("FAILED → Document Share | headerId={} reason=No files shared", request.getDocumentHeaderId());
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setMessage("Failed to share any files with any recipient");
            }
            response.setResponse(shareResponse);

            log.info("Document share process completed. Created {} share records, {} success, {} failures",
                    totalSharesCreated, successCount, failureCount);

        } catch (Exception e) {
            log.error("FAILED → Share Document | error={}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to share document: " + e.getMessage());
            response.setResponse(shareResponse);
        }

        return response;
    }

    // ======================= BULK SHARE =======================
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW, timeout = 60)
    public ApiResponse<DocumentShareResponse> bulkShareDocuments(BulkShareRequest request,
                                                                 HttpServletRequest httpRequest) {

        log.info("API CALL → Bulk Share Documents | documents={} recipients={}",
                request.getDocumentHeaderIds() != null ? request.getDocumentHeaderIds().size() : 0,
                request.getRecipientIds() != null ? request.getRecipientIds().size() : 0);

        ApiResponse<DocumentShareResponse> response = new ApiResponse<>();
        DocumentShareResponse shareResponse = new DocumentShareResponse();
        List<DocumentShareResponse.ShareResult> results = new ArrayList<>();

        try {
            // === VALIDATION PHASE ===
            if (request == null || request.getDocumentHeaderIds() == null || request.getDocumentHeaderIds().isEmpty()) {
                log.info("FAILED → Bulk Share | reason=Header IDs required");
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setMessage("Document Header IDs are required");
                return response;
            }

            if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
                log.info("FAILED → Bulk Share | reason=No recipients specified");
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setMessage("No recipients specified");
                return response;
            }

            // Get current user
            Employee currentEmployee;
            try {
                currentEmployee = currentUser.getCurrentEmployeeOrThrow();
                log.debug("Current user for bulk share: {} (ID: {})", currentEmployee.getName(), currentEmployee.getId());
            } catch (Exception e) {
                log.info("FAILED → Bulk Share | reason=User not authenticated");
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setMessage("User not authenticated");
                return response;
            }

            Integer currentDepartmentId = currentEmployee.getDepartment().getId();

            Map<Integer, DocumentHeader> documentHeaders = new HashMap<>();
            Map<Integer, List<DocumentDetails>> approvedFilesByHeader = new HashMap<>();

            // Fetch all document headers and their approved files
            for (Integer headerId : request.getDocumentHeaderIds()) {
                try {
                    DocumentHeader header = documentHeaderRepository.findById(headerId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Document header not found with id: " + headerId));

                    documentHeaders.put(headerId, header);

                    // Get ALL approved files for this document header
                    List<DocumentDetails> approvedFiles = header.getDocumentDetails().stream()
                            .filter(doc -> doc.getStatus() == DocApprovalStatus.APPROVED)
                            .filter(doc -> !Boolean.TRUE.equals(doc.getIsDeleted()))
                            .collect(Collectors.toList());

                    approvedFilesByHeader.put(headerId, approvedFiles);

                    if (approvedFiles.isEmpty()) {
                        log.warn("Document header {} has no approved files", headerId);
                    } else {
                        log.debug("Document header {} has {} approved files", headerId, approvedFiles.size());
                    }

                } catch (ResourceNotFoundException e) {
                    log.warn("Document header {} not found: {}", headerId, e.getMessage());
                }
            }

            if (documentHeaders.isEmpty()) {
                log.info("FAILED → Bulk Share | reason=No valid document headers found");
                response.setStatus(HttpStatus.NOT_FOUND.value());
                response.setMessage("No valid document headers found");
                return response;
            }

            // === SHARING PHASE ===
            int totalSharesCreated = 0;
            Map<Integer, Integer> sharesPerDocument = new HashMap<>();

            for (Integer recipientId : request.getRecipientIds()) {
                DocumentShareResponse.ShareResult result = new DocumentShareResponse.ShareResult();

                try {
                    // Get recipient employee
                    Employee recipient = employeeRepository.findById(recipientId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Recipient not found with id: " + recipientId));

                    result.setEmployeeName(recipient.getName());

                    // Validate recipient
                    String validationError = validateRecipient(recipient, currentEmployee, currentDepartmentId);
                    if (validationError != null) {
                        result.setSuccess(false);
                        result.setMessage(validationError);
                        results.add(result);
                        log.debug("Bulk share recipient validation failed for {}: {}", recipient.getName(), validationError);
                        continue;
                    }

                    int totalFilesSharedForRecipient = 0;
                    int totalFilesSkippedForRecipient = 0;
                    Map<Integer, Integer> filesSharedPerDocumentForRecipient = new HashMap<>();

                    // For each document header
                    for (Map.Entry<Integer, DocumentHeader> entry : documentHeaders.entrySet()) {
                        Integer headerId = entry.getKey();
                        DocumentHeader header = entry.getValue();
                        List<DocumentDetails> approvedFiles = approvedFilesByHeader.get(headerId);

                        if (approvedFiles == null || approvedFiles.isEmpty()) {
                            continue;
                        }

                        int filesSharedForThisDocument = 0;
                        int filesSkippedForThisDocument = 0;

                        // Share each approved file
                        for (DocumentDetails document : approvedFiles) {
                            // Check if this specific file is already shared with this employee
                            if (isFileAlreadyShared(document.getId(), recipientId)) {
                                filesSkippedForThisDocument++;
                                totalFilesSkippedForRecipient++;
                                continue;
                            }

                            // Create new share record for this file
                            DocumentShare share = createShare(
                                    document,
                                    header,
                                    currentEmployee,
                                    recipient,
                                    request.getEndTime()
                            );

                            DocumentShare savedShare = documentShareRepository.save(share);
                            totalSharesCreated++;
                            filesSharedForThisDocument++;
                            totalFilesSharedForRecipient++;

                            // Track shares per document
                            sharesPerDocument.put(headerId,
                                    sharesPerDocument.getOrDefault(headerId, 0) + 1);

                            // Track files shared per document for this recipient
                            filesSharedPerDocumentForRecipient.put(headerId,
                                    filesSharedPerDocumentForRecipient.getOrDefault(headerId, 0) + 1);

                            // Send notification immediately (synchronous)
                            sendNotification(savedShare);

                            // Log audit trail asynchronously
                            logAuditAsync(currentEmployee, header, document, recipient,
                                    savedShare, request.getEndTime(), httpRequest);
                        }

                        if (filesSkippedForThisDocument > 0) {
                            log.debug("For document {}, skipped {} already shared files for recipient {}",
                                    headerId, filesSkippedForThisDocument, recipientId);
                        }
                    }

                    if (totalFilesSharedForRecipient > 0) {
                        result.setSuccess(true);
                        result.setMessage(String.format(
                                "Shared %d file(s) across %d document(s). %d file(s) were already shared.",
                                totalFilesSharedForRecipient,
                                filesSharedPerDocumentForRecipient.size(),
                                totalFilesSkippedForRecipient
                        ));
                        log.debug("Bulk share successful for recipient {}: {} files",
                                recipient.getName(), totalFilesSharedForRecipient);
                    } else if (totalFilesSkippedForRecipient > 0) {
                        result.setSuccess(false);
                        result.setMessage("All files were already shared with this employee");
                        log.debug("All files already shared with recipient {}", recipient.getName());
                    } else {
                        result.setSuccess(false);
                        result.setMessage("No files could be shared");
                    }

                    results.add(result);

                } catch (ResourceNotFoundException e) {
                    log.warn("Resource not found for recipient {}: {}", recipientId, e.getMessage());
                    result.setEmployeeName("Recipient ID: " + recipientId);
                    result.setSuccess(false);
                    result.setMessage(e.getMessage());
                    results.add(result);
                } catch (Exception e) {
                    log.error("Unexpected error sharing with recipient {}: {}", recipientId, e.getMessage(), e);
                    result.setEmployeeName("Recipient ID: " + recipientId);
                    result.setSuccess(false);
                    result.setMessage("Error: " + e.getMessage());
                    results.add(result);
                }
            }

            // === RESPONSE PHASE ===
            long successCount = results.stream()
                    .filter(DocumentShareResponse.ShareResult::isSuccess)
                    .count();
            long failureCount = results.size() - successCount;

            shareResponse.setSharedBy(currentEmployee.getName());
            shareResponse.setSharedTo(results.stream()
                    .filter(DocumentShareResponse.ShareResult::isSuccess)
                    .map(DocumentShareResponse.ShareResult::getEmployeeName)
                    .collect(Collectors.toList()));
            shareResponse.setDocumentName("Multiple Documents");
            shareResponse.setCreatedAt(LocalDateTime.now());
            shareResponse.setEndTime(request.getEndTime());
            shareResponse.setResults(results);
            shareResponse.setTotalFilesShared(totalSharesCreated);
            shareResponse.setTotalDocuments(documentHeaders.size());

            // Generate detailed status
            StringBuilder statusBuilder = new StringBuilder();
            statusBuilder.append(String.format(
                    "Created %d total share records across %d document(s). ",
                    totalSharesCreated, documentHeaders.size()));

            for (Map.Entry<Integer, Integer> entry : sharesPerDocument.entrySet()) {
                DocumentHeader header = documentHeaders.get(entry.getKey());
                statusBuilder.append(String.format(
                        "Doc '%s': %d files; ",
                        header.getTitle(),
                        entry.getValue()));
            }

            statusBuilder.append(String.format(
                    "Successfully shared with %d employee(s), %d failed",
                    successCount, failureCount));

            shareResponse.setStatus(statusBuilder.toString());

            // Set HTTP response status
            if (successCount > 0 || totalSharesCreated > 0) {
                response.setStatus(HttpStatus.OK.value());
                response.setMessage(String.format(
                        "Shared %d file(s) across %d document(s) with %d recipient(s)",
                        totalSharesCreated,
                        documentHeaders.size(),
                        successCount
                ));
                log.info("SUCCESS → Bulk Share Completed | documents={} recipients={} sharesCreated={}",
                        documentHeaders.size(), successCount, totalSharesCreated);
            } else {
                log.info("FAILED → Bulk Share | reason=No files shared with any recipient");
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setMessage("Failed to share any files with any recipient");
            }
            response.setResponse(shareResponse);

            log.info("Bulk share process completed. Created {} share records across {} documents, {} success, {} failures",
                    totalSharesCreated, documentHeaders.size(), successCount, failureCount);

        } catch (Exception e) {
            log.error("FAILED → Bulk Share | error={}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to share documents: " + e.getMessage());
            response.setResponse(shareResponse);
        }

        return response;
    }

    // ======================= GET SHARED DOCUMENTS FOR CURRENT USER =======================
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<DocumentDetails> getSharedDocumentsForCurrentUser() {

        log.info("API CALL → Get Shared Documents For Current User");

        try {
            Employee currentEmployee = currentUser.getCurrentEmployeeOrThrow();
            LocalDateTime now = LocalDateTime.now();

            // Get all active shares for current user
            List<DocumentShare> activeShares = documentShareRepository.findActiveSharesForRecipient(
                    currentEmployee.getId(),
                    now
            );

            // Extract unique document details that are approved and not deleted
            List<DocumentDetails> sharedDocs = activeShares.stream()
                    .map(DocumentShare::getDocumentDetails)
                    .filter(Objects::nonNull)
                    .filter(doc -> doc.getStatus() == DocApprovalStatus.APPROVED)
                    .filter(doc -> !Boolean.TRUE.equals(doc.getIsDeleted()))
                    .distinct()
                    .collect(Collectors.toList());

            log.info("SUCCESS → Retrieved {} shared documents for user {}",
                    sharedDocs.size(), currentEmployee.getName());

            return sharedDocs;
        } catch (Exception e) {
            log.error("FAILED → Get Shared Documents | error={}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // ======================= GET DOCUMENTS SHARED BY CURRENT USER =======================
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<DocumentShareResponse.ShareInfoResponse> getDocumentsSharedByCurrentUser() {

        log.info("API CALL → Get Documents Shared By Current User");

        try {
            Employee currentEmployee = currentUser.getCurrentEmployeeOrThrow();
            LocalDateTime now = LocalDateTime.now();

            // Get all shares created by current user
            List<DocumentShare> shares = documentShareRepository.findBySharedByIdAndIsActiveTrue(currentEmployee.getId());

            // Group by document header and recipient for better organization
            Map<Integer, Map<Integer, List<DocumentShare>>> sharesByHeaderAndRecipient = new HashMap<>();

            for (DocumentShare share : shares) {
                if (share.getDocumentHeader() != null && share.getSharedTo() != null) {
                    Integer headerId = share.getDocumentHeader().getId();
                    Integer recipientId = share.getSharedTo().getId();

                    sharesByHeaderAndRecipient
                            .computeIfAbsent(headerId, k -> new HashMap<>())
                            .computeIfAbsent(recipientId, k -> new ArrayList<>())
                            .add(share);
                }
            }

            List<DocumentShareResponse.ShareInfoResponse> responses = new ArrayList<>();

            // Convert grouped shares to response
            for (Map.Entry<Integer, Map<Integer, List<DocumentShare>>> headerEntry : sharesByHeaderAndRecipient.entrySet()) {
                for (Map.Entry<Integer, List<DocumentShare>> recipientEntry : headerEntry.getValue().entrySet()) {
                    if (!recipientEntry.getValue().isEmpty()) {
                        DocumentShare firstShare = recipientEntry.getValue().get(0);
                        DocumentShareResponse.ShareInfoResponse response = DocumentShareResponse.ShareInfoResponse.fromEntity(firstShare);

                        // Add additional info
                        if (response != null) {
                            response.setTotalFilesShared(recipientEntry.getValue().size());
                            response.setSharedFileNames(recipientEntry.getValue().stream()
                                    .map(s -> s.getDocumentDetails().getDocName())
                                    .collect(Collectors.toList()));
                            responses.add(response);
                        }
                    }
                }
            }

            log.info("SUCCESS → Retrieved {} shared document entries for user {}",
                    responses.size(), currentEmployee.getName());

            return responses;
        } catch (Exception e) {
            log.error("FAILED → Get Documents Shared By User | error={}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // ======================= GET SHARES FOR DOCUMENT =======================
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<DocumentShareResponse.ShareInfoResponse> getSharesForDocument(Integer documentHeaderId) {

        log.info("API CALL → Get Shares For Document | documentId={}", documentHeaderId);

        try {
            List<DocumentShare> shares = documentShareRepository.findByDocumentHeaderIdAndIsActiveTrue(documentHeaderId);

            // Group by recipient
            Map<Integer, List<DocumentShare>> sharesByRecipient = new HashMap<>();

            for (DocumentShare share : shares) {
                if (share.getSharedTo() != null) {
                    sharesByRecipient
                            .computeIfAbsent(share.getSharedTo().getId(), k -> new ArrayList<>())
                            .add(share);
                }
            }

            List<DocumentShareResponse.ShareInfoResponse> responses = new ArrayList<>();

            // Convert grouped shares to response
            for (Map.Entry<Integer, List<DocumentShare>> entry : sharesByRecipient.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    DocumentShare firstShare = entry.getValue().get(0);
                    DocumentShareResponse.ShareInfoResponse response = DocumentShareResponse.ShareInfoResponse.fromEntity(firstShare);

                    if (response != null) {
                        response.setTotalFilesShared(entry.getValue().size());
                        response.setSharedFileNames(entry.getValue().stream()
                                .map(s -> s.getDocumentDetails().getDocName())
                                .collect(Collectors.toList()));
                        response.setShareIds(entry.getValue().stream()
                                .map(DocumentShare::getId)
                                .collect(Collectors.toList()));
                        responses.add(response);
                    }
                }
            }

            log.info("SUCCESS → Retrieved {} share entries for document {}",
                    responses.size(), documentHeaderId);

            return responses;
        } catch (Exception e) {
            log.error("FAILED → Get Shares For Document | documentId={} error={}", documentHeaderId, e.getMessage());
            return new ArrayList<>();
        }
    }

    // ======================= REVOKE SHARE =======================
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<MessageResponse> revokeShare(ShareRevokeRequest request, HttpServletRequest httpRequest) {

        log.info("API CALL → Revoke Share | shareId={}", request.getShareId());

        ApiResponse<MessageResponse> response = new ApiResponse<>();
        MessageResponse message = new MessageResponse();

        try {
            if (request == null || request.getShareId() == null) {
                log.info("FAILED → Revoke Share | reason=Share ID is required");
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                message.setMsg("Share ID is required");
                response.setResponse(message);
                return response;
            }

            Employee currentEmployee = currentUser.getCurrentEmployeeOrThrow();

            DocumentShare share = documentShareRepository.findById(Long.valueOf(request.getShareId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Share record not found"));

            // Check if current user is the one who shared it
            if (!share.getSharedBy().getId().equals(currentEmployee.getId())) {
                log.info("FAILED → Revoke Share | shareId={} reason=Not authorized to revoke", request.getShareId());
                response.setStatus(HttpStatus.FORBIDDEN.value());
                message.setMsg("You can only revoke shares you created");
                response.setResponse(message);
                return response;
            }

            // Soft delete by setting isActive to false
            share.setIsActive(false);
            documentShareRepository.save(share);

            log.info("SUCCESS → Share Revoked | shareId={} document={} recipient={}",
                    share.getId(), share.getDocumentDetails().getDocName(), share.getSharedTo().getName());

            // Send revoke notification immediately (synchronous)
            sendRevokeNotification(share, request.getReason());

            // Log audit trail asynchronously
            logRevokeAuditAsync(currentEmployee, share, request.getReason(), httpRequest);

            response.setStatus(HttpStatus.OK.value());
            message.setMsg("Document share revoked successfully");
            response.setResponse(message);

        } catch (ResourceNotFoundException e) {
            log.error("FAILED → Revoke Share | shareId={} reason=Share not found", request.getShareId());
            response.setStatus(HttpStatus.NOT_FOUND.value());
            message.setMsg(e.getMessage());
            response.setResponse(message);
        } catch (Exception e) {
            log.error("FAILED → Revoke Share | shareId={} error={}", request.getShareId(), e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            message.setMsg("Failed to revoke share: " + e.getMessage());
            response.setResponse(message);
        }

        return response;
    }

    // ======================= IS DOCUMENT SHARED WITH USER =======================
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public boolean isDocumentSharedWithUser(Integer documentId) {

        log.debug("API CALL → Check Document Shared With User | documentId={}", documentId);

        try {
            Employee currentEmployee = currentUser.getCurrentEmployeeOrThrow();
            LocalDateTime now = LocalDateTime.now();

            Optional<DocumentShare> existingShare = documentShareRepository.findExistingActiveShare(
                    documentId,
                    currentEmployee.getId(),
                    now
            );

            boolean isShared = existingShare.isPresent();
            log.debug("Document {} shared with user {}: {}", documentId, currentEmployee.getName(), isShared);

            return isShared;
        } catch (Exception e) {
            log.error("FAILED → Check Document Shared | documentId={} error={}", documentId, e.getMessage());
            return false;
        }
    }

    // ======================= GET SAME DEPARTMENT EMPLOYEES =======================
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Employee> getSameDepartmentEmployees() {

        log.info("API CALL → Get Same Department Employees");

        try {
            Employee currentEmployee = currentUser.getCurrentEmployeeOrThrow();
            Integer departmentId = currentEmployee.getDepartment().getId();

            List<Employee> departmentEmployees = employeeRepository.findByDepartmentId(departmentId).stream()
                    .filter(emp -> !emp.getId().equals(currentEmployee.getId()))
                    .filter(Employee::isActive)
                    .collect(Collectors.toList());

            log.info("SUCCESS → Retrieved {} employees from department {}",
                    departmentEmployees.size(), currentEmployee.getDepartment().getName());

            return departmentEmployees;
        } catch (Exception e) {
            log.error("FAILED → Get Department Employees | error={}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ======================= CLEANUP EXPIRED SHARES =======================
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void cleanupExpiredShares() {

        log.info("API CALL → Cleanup Expired Shares");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<DocumentShare> expiredShares = documentShareRepository.findAll().stream()
                    .filter(share -> share.getIsActive() &&
                            share.getEndTime() != null &&
                            share.getEndTime().isBefore(now))
                    .collect(Collectors.toList());

            for (DocumentShare share : expiredShares) {
                share.setIsActive(false);
                log.debug("Auto-deactivated expired share ID: {}", share.getId());
            }

            if (!expiredShares.isEmpty()) {
                documentShareRepository.saveAll(expiredShares);
                log.info("SUCCESS → Cleaned up {} expired shares", expiredShares.size());
            } else {
                log.info("No expired shares found to cleanup");
            }
        } catch (Exception e) {
            log.error("FAILED → Cleanup Expired Shares | error={}", e.getMessage(), e);
        }
    }

    // ======================= GET DOCUMENTS SHARED WITH CURRENT USER =======================
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<DocumentShareResponse.ShareInfoResponse> getDocumentsSharedWithCurrentUser() {

        log.info("API CALL → Get Documents Shared With Current User");

        try {
            Employee currentEmployee = currentUser.getCurrentEmployeeOrThrow();
            LocalDateTime now = LocalDateTime.now();

            // Get all active shares where current user is the recipient
            List<DocumentShare> shares = documentShareRepository.findActiveSharesForRecipient(
                    currentEmployee.getId(),
                    now
            );

            // Group by document header and sharer for better organization
            Map<Integer, Map<Integer, List<DocumentShare>>> sharesByHeaderAndSharer = new HashMap<>();

            for (DocumentShare share : shares) {
                if (share.getDocumentHeader() != null && share.getSharedBy() != null) {
                    Integer headerId = share.getDocumentHeader().getId();
                    Integer sharerId = share.getSharedBy().getId();

                    sharesByHeaderAndSharer
                            .computeIfAbsent(headerId, k -> new HashMap<>())
                            .computeIfAbsent(sharerId, k -> new ArrayList<>())
                            .add(share);
                }
            }

            List<DocumentShareResponse.ShareInfoResponse> responses = new ArrayList<>();

            // Convert grouped shares to response
            for (Map.Entry<Integer, Map<Integer, List<DocumentShare>>> headerEntry : sharesByHeaderAndSharer.entrySet()) {
                for (Map.Entry<Integer, List<DocumentShare>> sharerEntry : headerEntry.getValue().entrySet()) {
                    if (!sharerEntry.getValue().isEmpty()) {
                        DocumentShare firstShare = sharerEntry.getValue().get(0);
                        DocumentShareResponse.ShareInfoResponse response = DocumentShareResponse.ShareInfoResponse.fromEntity(firstShare);

                        if (response != null) {
                            response.setTotalFilesShared(sharerEntry.getValue().size());
                            response.setSharedFileNames(sharerEntry.getValue().stream()
                                    .map(s -> s.getDocumentDetails().getDocName())
                                    .collect(Collectors.toList()));
                            response.setShareIds(sharerEntry.getValue().stream()
                                    .map(DocumentShare::getId)
                                    .collect(Collectors.toList()));
                            responses.add(response);
                        }
                    }
                }
            }

            log.info("SUCCESS → Retrieved {} document shares for current user", responses.size());

            return responses;
        } catch (Exception e) {
            log.error("FAILED → Get Documents Shared With User | error={}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // ======================= GET SHARED DOCUMENT HEADERS FOR CURRENT USER =======================
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<DocumentHeader> getSharedDocumentHeadersForCurrentUser() {

        log.info("API CALL → Get Shared Document Headers For Current User");

        try {
            Employee currentEmployee = currentUser.getCurrentEmployeeOrThrow();
            LocalDateTime now = LocalDateTime.now();

            List<DocumentShare> activeShares = documentShareRepository.findActiveSharesForRecipient(
                    currentEmployee.getId(),
                    now
            );

            // Group by document header
            Map<Long, DocumentHeader> headersMap = new HashMap<>();
            Map<Long, List<DocumentDetails>> detailsMap = new HashMap<>();

            for (DocumentShare share : activeShares) {
                DocumentHeader header = share.getDocumentHeader();
                DocumentDetails details = share.getDocumentDetails();

                if (header != null && details != null &&
                        details.getStatus() == DocApprovalStatus.APPROVED &&
                        !Boolean.TRUE.equals(details.getIsDeleted())) {

                    if (!headersMap.containsKey(header.getId())) {
                        // Initialize header and its relationships
                        if (header.getDepartmentMaster() != null) {
                            header.getDepartmentMaster().getName();
                        }
                        if (header.getCategoryMaster() != null) {
                            header.getCategoryMaster().getName();
                        }
                        headersMap.put(Long.valueOf(header.getId()), header);
                        detailsMap.put(Long.valueOf(header.getId()), new ArrayList<>());
                    }

                    // Add document details if not already added
                    if (detailsMap.get(header.getId()).stream()
                            .noneMatch(d -> d.getId().equals(details.getId()))) {
                        // Initialize details relationships
                        if (details.getYearMaster() != null) {
                            details.getYearMaster().getName();
                        }
                        detailsMap.get(header.getId()).add(details);
                    }
                }
            }

            // Set document details for each header
            List<DocumentHeader> headers = new ArrayList<>(headersMap.values());
            for (DocumentHeader header : headers) {
                List<DocumentDetails> details = detailsMap.get(header.getId());
                header.setDocumentDetails(details != null ? details : new ArrayList<>());
            }

            log.info("SUCCESS → Retrieved {} shared document headers for user {}",
                    headers.size(), currentEmployee.getName());

            return headers;
        } catch (Exception e) {
            log.error("FAILED → Get Shared Document Headers | error={}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ======================= HAS DOCUMENT SHARES =======================
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public boolean hasDocumentShares(Integer documentHeaderId) {

        log.debug("API CALL → Check Document Has Shares | documentId={}", documentHeaderId);

        try {
            long shareCount = documentShareRepository.countByDocumentHeaderIdAndIsActiveTrue(documentHeaderId);
            boolean hasShares = shareCount > 0;

            log.debug("Document {} has shares: {} (count: {})", documentHeaderId, hasShares, shareCount);

            return hasShares;
        } catch (Exception e) {
            log.error("FAILED → Check Document Shares | documentId={} error={}", documentHeaderId, e.getMessage());
            return false;
        }
    }

    // ======================= GET SHARE COUNT FOR DOCUMENT =======================
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public int getShareCountForDocument(Integer documentHeaderId) {

        log.debug("API CALL → Get Share Count For Document | documentId={}", documentHeaderId);

        try {
            int shareCount = documentShareRepository.countByDocumentHeaderIdAndIsActiveTrue(documentHeaderId);

            log.debug("Document {} share count: {}", documentHeaderId, shareCount);

            return shareCount;
        } catch (Exception e) {
            log.error("FAILED → Get Share Count | documentId={} error={}", documentHeaderId, e.getMessage());
            return 0;
        }
    }

    // ======================= HELPER METHODS =======================

    /**
     * Helper method to validate recipient
     */
    private String validateRecipient(Employee recipient, Employee currentEmployee, Integer currentDepartmentId) {
        // Check if recipient exists
        if (recipient == null) {
            return "Recipient not found";
        }

        // Check if recipient has department
        if (recipient.getDepartment() == null) {
            return "Recipient has no department assigned";
        }

        // Check if recipient is in the same department
        if (!recipient.getDepartment().getId().equals(currentDepartmentId)) {
            return "Cannot share with employees from different department";
        }

        // Check if recipient is active
        if (!recipient.isActive()) {
            return "Cannot share with inactive employees";
        }

        // Check if recipient is the same as sharer
        if (recipient.getId().equals(currentEmployee.getId())) {
            return "Cannot share document with yourself";
        }

        return null; // No validation errors
    }

    /**
     * Check if a specific file is already shared with a recipient
     */
    private boolean isFileAlreadyShared(Integer documentId, Integer recipientId) {
        try {
            Optional<DocumentShare> existingShare = documentShareRepository.findExistingActiveShare(
                    documentId,
                    recipientId,
                    LocalDateTime.now()
            );
            return existingShare.isPresent();
        } catch (Exception e) {
            log.error("Error checking if file is already shared: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Create a new DocumentShare entity
     */
    private DocumentShare createShare(DocumentDetails document, DocumentHeader documentHeader,
                                      Employee sharedBy, Employee sharedTo, LocalDateTime endTime) {
        DocumentShare share = new DocumentShare();
        share.setDocumentDetails(document);
        share.setDocumentHeader(documentHeader);
        share.setSharedBy(sharedBy);
        share.setSharedTo(sharedTo);
        share.setEndTime(endTime);
        share.setIsActive(true);
        share.setCreatedAt(LocalDateTime.now());
        return share;
    }

    /**
     * Send notification synchronously to ensure it's created
     */
    private void sendNotification(DocumentShare share) {
        try {
            log.debug("Sending notification for share ID: {}, recipient: {}",
                    share.getId(), share.getSharedTo().getName());
            notificationService.createDocumentShareNotification(share);
            log.debug("Notification sent successfully for share ID: {}", share.getId());
        } catch (Exception e) {
            log.error("Failed to send notification for share {}: {}", share.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send revoke notification synchronously to ensure it's created
     */
    private void sendRevokeNotification(DocumentShare share, String reason) {
        try {
            log.debug("Sending revoke notification for share ID: {}, recipient: {}",
                    share.getId(), share.getSharedTo().getName());
            notificationService.createDocumentShareRevokeNotification(share, reason);
            log.debug("Revoke notification sent successfully for share ID: {}", share.getId());
        } catch (Exception e) {
            log.error("Failed to send revoke notification for share {}: {}", share.getId(), e.getMessage(), e);
        }
    }

    /**
     * Log audit trail asynchronously
     */
    @Async
    protected void logAuditAsync(Employee currentEmployee,
                                 DocumentHeader documentHeader,
                                 DocumentDetails document,
                                 Employee recipient,
                                 DocumentShare share,
                                 LocalDateTime endTime,
                                 HttpServletRequest httpRequest) {
        try {
            log.debug("Logging audit trail for share ID: {}", share.getId());

            DocumentDetailsResponse fileDetail = new DocumentDetailsResponse();
            fileDetail.setId(document.getId());
            fileDetail.setDocName(document.getDocName());

            auditLogUtil.logDocumentAction(
                    currentEmployee,
                    "DocumentShare",
                    "Share",
                    "Success",
                    documentHeader.getId(),
                    List.of(fileDetail),
                    Map.of(
                            "shareId", share.getId(),
                            "recipientId", recipient.getId(),
                            "recipientName", recipient.getName(),
                            "endTime", endTime != null ? endTime.toString() : "Permanent"
                    ),
                    httpRequest
            );

            log.debug("Audit logged successfully for share ID: {}", share.getId());
        } catch (Exception e) {
            log.warn("Failed to log share audit {}: {}", share.getId(), e.getMessage());
        }
    }

    /**
     * Log revoke audit trail asynchronously
     */
    @Async
    protected void logRevokeAuditAsync(Employee currentEmployee,
                                       DocumentShare share,
                                       String reason,
                                       HttpServletRequest httpRequest) {
        try {
            log.debug("Logging revoke audit trail for share ID: {}", share.getId());

            DocumentDetailsResponse fileDetail = new DocumentDetailsResponse();
            fileDetail.setId(share.getDocumentDetails().getId());
            fileDetail.setDocName(share.getDocumentDetails().getDocName());

            auditLogUtil.logDocumentAction(
                    currentEmployee,
                    "DocumentShare",
                    "Revoke",
                    "Success",
                    share.getDocumentHeader().getId(),
                    List.of(fileDetail),
                    Map.of(
                            "shareId", share.getId(),
                            "recipientId", share.getSharedTo().getId(),
                            "recipientName", share.getSharedTo().getName(),
                            "reason", reason != null ? reason : "No reason provided"
                    ),
                    httpRequest
            );

            log.debug("Revoke audit logged successfully for share ID: {}", share.getId());
        } catch (Exception e) {
            log.warn("Failed to log revoke audit {}: {}", share.getId(), e.getMessage());
        }
    }
}