package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.*;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.repository.*;
import com.dmsBackend.response.*;
import com.dmsBackend.service.ReportEntryService;
import com.dmsBackend.utils.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportEntryServiceImpl implements ReportEntryService {

    @Autowired private DocumentHeaderRepository documentHeaderRepository;
    @Autowired private DocumentDetailsRepository documentDetailsRepository;
    @Autowired private ReportEntryRepository reportEntryRepository;
    @Autowired private ReportEntryAttachmentRepository reportEntryAttachmentRepository;
    @Autowired private ExaminationMethodMasterRepository examinationMethodMasterRepository;
    @Autowired private DepartmentMasterRepository departmentMasterRepository;
    @Autowired private CurrentUser currentUser;

    @Value("${report.storage.path:${document.storage.path}/reports}")
    private String reportStoragePath;

    @Override
    @Transactional(readOnly = true)
    public List<ReportEntryListItem> getMyPendingReportEntries() {
        Employee me = currentUser.getCurrentEmployeeOrThrow();

        log.info("API CALL → Get My Pending Report Entries | employeeId={}", me.getId());

        List<DocumentDetails> myAssignedEvidence =
                documentDetailsRepository.findByEmpId_Id(me.getId());

        log.info("Found {} evidence records assigned to employee {}", myAssignedEvidence.size(), me.getId());

        Map<Integer, List<DocumentDetails>> byHeader =
                myAssignedEvidence.stream()
                        .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                        .filter(d -> d.getDocumentHeader() != null)
                        .collect(Collectors.groupingBy(
                                d -> d.getDocumentHeader().getId()
                        ));

        log.info("Grouped into {} unique headers", byHeader.size());

        List<ReportEntryListItem> result = new ArrayList<>();

        for (Map.Entry<Integer, List<DocumentDetails>> entry : byHeader.entrySet()) {

            DocumentHeader header =
                    entry.getValue().get(0).getDocumentHeader();

            // Skip if case is not in PENDING or APPROVED status
            if (header.getApprovalStatus() != DocApprovalStatus.PENDING &&
                    header.getApprovalStatus() != DocApprovalStatus.APPROVED) {
                log.info("Skipping header {} with status {}", header.getId(), header.getApprovalStatus());
                continue;
            }

            Optional<ReportEntry> existing =
                    reportEntryRepository.findByDocumentHeader_IdAndEmployee_Id(
                            header.getId(),
                            me.getId()
                    );

            // Skip already submitted reports
            if (existing.isPresent()
                    && existing.get().getStatus() == ReportEntry.ReportStatus.SUBMITTED) {
                log.info("Skipping header {} - already submitted", header.getId());
                continue;
            }

            ReportEntryListItem item = new ReportEntryListItem();

            item.setDocumentHeaderId(header.getId());
            item.setFileNo(header.getFileNo());
            item.setTitle(header.getTitle());

            // Access lazy relationships safely within transaction
            try {
                if (header.getCaseType() != null) {
                    item.setCaseType(header.getCaseType().getName());
                } else {
                    item.setCaseType(null);
                }
            } catch (Exception e) {
                log.warn("Could not access caseType for header {}", header.getId());
                item.setCaseType(null);
            }

            item.setAttachedFileCount(entry.getValue().size());

            item.setStatus(
                    existing.map(r -> r.getStatus().name())
                            .orElse("PENDING")
            );

            result.add(item);
        }

        log.info(
                "SUCCESS → Found {} cases pending report entry for employee {}",
                result.size(),
                me.getId()
        );

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportEntryCaseResponse getCaseForReportEntry(Integer documentHeaderId) {

        Employee me = currentUser.getCurrentEmployeeOrThrow();

        log.info(
                "API CALL → Get Case For Report Entry | documentHeaderId={} employeeId={}",
                documentHeaderId,
                me.getId()
        );

        try {
            DocumentHeader header = documentHeaderRepository
                    .findById(documentHeaderId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Case not found: " + documentHeaderId
                            )
                    );

            log.info(
                    "Found header: id={}, caseId={}, fileNo={}, title={}",
                    header.getId(),
                    header.getCaseId(),
                    header.getFileNo(),
                    header.getTitle()
            );

            // Get all details for this header using repository
            List<DocumentDetails> allDetails = documentDetailsRepository.findByDocumentHeaderId(documentHeaderId);

            log.info("Total document details for header {}: {}", documentHeaderId, allDetails.size());

            // Log all details for debugging
            for (DocumentDetails d : allDetails) {
                log.info("  Detail: id={}, empId={}, isDeleted={}, docName={}",
                        d.getId(),
                        d.getEmpId() != null ? d.getEmpId().getId() : null,
                        d.getIsDeleted(),
                        d.getDocName());
            }

            // Filter to my evidence
            List<DocumentDetails> myEvidence = allDetails.stream()
                    .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                    .filter(d -> d.getEmpId() != null && me.getId().equals(d.getEmpId().getId()))
                    .collect(Collectors.toList());

            log.info("Employee {} has {} assigned evidence files for header {}",
                    me.getId(), myEvidence.size(), documentHeaderId);

            // Build response even if no evidence
            ReportEntryCaseResponse resp = new ReportEntryCaseResponse();
            resp.setDocumentHeaderId(header.getId());
            resp.setCaseId(header.getCaseId());
            resp.setFileNo(header.getFileNo());
            resp.setTitle(header.getTitle());
            resp.setSubject(header.getSubject());
            resp.setFirNumber(header.getFirNumber());
            resp.setPoliceStation(header.getPoliceStation());

            if (myEvidence.isEmpty()) {
                log.warn("No evidence assigned to employee {} in case {}", me.getId(), documentHeaderId);
                resp.setMyEvidenceList(new ArrayList<>());
                resp.setReportStatus("NOT_STARTED");
                return resp;
            }

            // Get division name
            try {
                if (myEvidence.get(0).getDepartment() != null) {
                    Integer divisionId = myEvidence.get(0).getDepartment().getId();
                    if (divisionId != null) {
                        departmentMasterRepository.findById(divisionId)
                                .ifPresent(dept -> resp.setDivisionName(dept.getName()));
                    }
                }
            } catch (Exception e) {
                log.warn("Could not access department: {}", e.getMessage());
            }

            // Build evidence rows
            List<ReportEntryCaseResponse.EvidenceRow> evidenceRows = myEvidence.stream()
                    .map(d -> {
                        ReportEntryCaseResponse.EvidenceRow row = new ReportEntryCaseResponse.EvidenceRow();
                        row.setDocumentDetailId(d.getId());
                        row.setDocName(d.getDocName());

                        try {
                            row.setEvidenceCategory(header.getCategoryMaster() != null
                                    ? header.getCategoryMaster().getName() : null);
                        } catch (Exception e) {
                            row.setEvidenceCategory(null);
                        }

                        try {
                            row.setEvidenceType(d.getEvidenceTypeId() != null
                                    ? d.getEvidenceTypeId().getName() : null);
                        } catch (Exception e) {
                            row.setEvidenceType(null);
                        }

                        row.setEvidenceDescription(d.getEvidenceDescription());
                        return row;
                    })
                    .collect(Collectors.toList());

            resp.setMyEvidenceList(evidenceRows);

            // Get existing report entry
            Optional<ReportEntry> existing = reportEntryRepository
                    .findByDocumentHeader_IdAndEmployee_Id(header.getId(), me.getId());

            resp.setReportStatus(existing.map(r -> r.getStatus().name()).orElse("NOT_STARTED"));

            existing.ifPresent(r -> {
                resp.setExaminationStartDate(r.getExaminationStartDate());
                resp.setExaminationEndDate(r.getExaminationEndDate());

                try {
                    resp.setExaminationMethodId(r.getExaminationMethod() != null
                            ? r.getExaminationMethod().getId() : null);
                } catch (Exception e) {
                    resp.setExaminationMethodId(null);
                }

                resp.setObservations(r.getObservations());
                resp.setScientificOpinion(r.getScientificOpinion());
                resp.setExaminationRemarks(r.getExaminationRemarks());
                resp.setReportDate(r.getReportDate());
                resp.setReportTitle(r.getReportTitle());
                resp.setReportSummary(r.getReportSummary());
                resp.setScientificReportPath(r.getScientificReportPath());
                resp.setSubmittedOn(r.getSubmittedOn());

                try {
                    if (r.getAttachments() != null) {
                        List<ReportEntryCaseResponse.AttachmentRow> attachments = r.getAttachments()
                                .stream()
                                .map(a -> {
                                    ReportEntryCaseResponse.AttachmentRow row = new ReportEntryCaseResponse.AttachmentRow();
                                    row.setId(a.getId());
                                    row.setFileName(a.getFileName());
                                    row.setFilePath(a.getFilePath());
                                    return row;
                                })
                                .collect(Collectors.toList());
                        resp.setAttachments(attachments);
                    } else {
                        resp.setAttachments(new ArrayList<>());
                    }
                } catch (Exception e) {
                    resp.setAttachments(new ArrayList<>());
                }
            });

            log.info("SUCCESS → Got case for report entry | documentHeaderId={} employeeId={}",
                    documentHeaderId, me.getId());

            return resp;

        } catch (ResourceNotFoundException e) {
            log.error("Resource not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting case: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get case details: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public ApiResponse<MessageResponse> saveReportEntry(
            Integer documentHeaderId,
            String examinationStartDate,
            String examinationEndDate,
            Integer examinationMethodId,
            String observations,
            String scientificOpinion,
            String examinationRemarks,
            String reportDate,
            String reportTitle,
            String reportSummary,
            String status,
            MultipartFile scientificReport,
            List<MultipartFile> supportingDocuments) {

        log.info("API CALL → Save Report Entry | documentHeaderId={} status={}", documentHeaderId, status);

        MessageResponse msg = new MessageResponse();
        ApiResponse<MessageResponse> api = new ApiResponse<>();
        Employee me = currentUser.getCurrentEmployeeOrThrow();

        try {
            DocumentHeader header = documentHeaderRepository.findById(documentHeaderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + documentHeaderId));

            log.info("Found header: id={}, fileNo={}, title={}", header.getId(), header.getFileNo(), header.getTitle());

            // FIX: Use repository to check assignment
            List<DocumentDetails> myAssignedEvidence = documentDetailsRepository
                    .findByEmpId_Id(me.getId())
                    .stream()
                    .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                    .filter(d -> d.getDocumentHeader() != null && d.getDocumentHeader().getId().equals(documentHeaderId))
                    .collect(Collectors.toList());

            log.info("Found {} evidence records for employee {} in case {}",
                    myAssignedEvidence.size(), me.getId(), documentHeaderId);

            // Log all evidence for this header to debug
            List<DocumentDetails> allHeaderDetails = documentDetailsRepository.findByDocumentHeaderId(documentHeaderId);
            log.info("Total evidence in case {}: {}", documentHeaderId, allHeaderDetails.size());
            for (DocumentDetails d : allHeaderDetails) {
                log.info("  Evidence: id={}, empId={}, isDeleted={}, docName={}",
                        d.getId(),
                        d.getEmpId() != null ? d.getEmpId().getId() : null,
                        d.getIsDeleted(),
                        d.getDocName());
            }

            if (myAssignedEvidence.isEmpty()) {
                log.warn("NO EVIDENCE FOUND for employee {} in case {}", me.getId(), documentHeaderId);

                msg.setMsg("No evidence assigned to you in this case");
                api.setStatus(HttpStatus.FORBIDDEN.value());
                api.setMessage(msg.getMsg());
                api.setResponse(msg);
                return api;
            }

            // Continue with saving...
            ReportEntry.ReportStatus targetStatus = "SUBMITTED".equalsIgnoreCase(status)
                    ? ReportEntry.ReportStatus.SUBMITTED
                    : ReportEntry.ReportStatus.DRAFT;

            Optional<ReportEntry> existingOpt = reportEntryRepository
                    .findByDocumentHeader_IdAndEmployee_Id(documentHeaderId, me.getId());

            if (existingOpt.isPresent() && existingOpt.get().getStatus() == ReportEntry.ReportStatus.SUBMITTED) {
                msg.setMsg("This report has already been submitted and cannot be edited");
                api.setStatus(HttpStatus.CONFLICT.value());
                api.setMessage(msg.getMsg());
                api.setResponse(msg);
                return api;
            }

            ReportEntry report = existingOpt.orElseGet(() -> {
                ReportEntry fresh = new ReportEntry();
                fresh.setDocumentHeader(header);
                fresh.setEmployee(me);
                fresh.setCreatedOn(new Timestamp(System.currentTimeMillis()));
                return fresh;
            });

            report.setExaminationStartDate(parseDateOrNull(examinationStartDate));
            report.setExaminationEndDate(parseDateOrNull(examinationEndDate));

            if (examinationMethodId != null) {
                ExaminationMethodMaster method = examinationMethodMasterRepository.findById(examinationMethodId)
                        .orElseThrow(() -> new ResourceNotFoundException("Examination method not found: " + examinationMethodId));
                report.setExaminationMethod(method);
            } else {
                report.setExaminationMethod(null);
            }

            report.setObservations(observations);
            report.setScientificOpinion(scientificOpinion);
            report.setExaminationRemarks(examinationRemarks);
            report.setReportDate(parseDateOrNull(reportDate));
            report.setReportTitle(reportTitle);
            report.setReportSummary(reportSummary);
            report.setStatus(targetStatus);
            report.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
            if (targetStatus == ReportEntry.ReportStatus.SUBMITTED) {
                report.setSubmittedOn(new Timestamp(System.currentTimeMillis()));
                // FIX: Set review_status to PENDING when report is submitted
                report.setReviewStatus("PENDING");
            }

            // Validate SUBMITTED requires core fields
            if (targetStatus == ReportEntry.ReportStatus.SUBMITTED) {
                if (report.getExaminationStartDate() == null || report.getExaminationEndDate() == null
                        || report.getExaminationMethod() == null || isBlank(report.getObservations())
                        || isBlank(report.getScientificOpinion()) || isBlank(report.getReportTitle())
                        || isBlank(report.getReportSummary())) {
                    msg.setMsg("Please fill in all required fields before submitting the report");
                    api.setStatus(HttpStatus.BAD_REQUEST.value());
                    api.setMessage(msg.getMsg());
                    api.setResponse(msg);
                    return api;
                }
            }

            Path caseDir = Paths.get(reportStoragePath, "case_" + documentHeaderId, "employee_" + me.getId());
            Files.createDirectories(caseDir);

            if (scientificReport != null && !scientificReport.isEmpty()) {
                String safeName = System.currentTimeMillis() + "_" +
                        scientificReport.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                Path dest = caseDir.resolve(safeName);
                try (InputStream in = scientificReport.getInputStream()) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }
                report.setScientificReportPath(dest.toString());
            }

            ReportEntry savedReport = reportEntryRepository.save(report);

            if (supportingDocuments != null) {
                for (MultipartFile file : supportingDocuments) {
                    if (file == null || file.isEmpty()) continue;
                    String safeName = System.currentTimeMillis() + "_" +
                            file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                    Path dest = caseDir.resolve(safeName);
                    try (InputStream in = file.getInputStream()) {
                        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                    }

                    ReportEntryAttachment attachment = new ReportEntryAttachment();
                    attachment.setReportEntry(savedReport);
                    attachment.setFileName(file.getOriginalFilename());
                    attachment.setFilePath(dest.toString());
                    attachment.setFileSizeBytes(file.getSize());
                    attachment.setUploadedOn(new Timestamp(System.currentTimeMillis()));
                    reportEntryAttachmentRepository.save(attachment);
                }
            }

            msg.setMsg(targetStatus == ReportEntry.ReportStatus.SUBMITTED
                    ? "Report submitted successfully"
                    : "Draft saved successfully");
            api.setStatus(HttpStatus.OK.value());
            api.setMessage("Success");
            api.setResponse(msg);

            log.info("SUCCESS → Report Entry Saved | documentHeaderId={} employeeId={} status={}",
                    documentHeaderId, me.getId(), targetStatus);

        } catch (IOException e) {
            log.error("FAILED → Save Report Entry (file IO) | reason={}", e.getMessage(), e);
            msg.setMsg("Failed to save report files: " + e.getMessage());
            api.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            api.setMessage(msg.getMsg());
            api.setResponse(msg);
        } catch (Exception e) {
            log.error("FAILED → Save Report Entry | reason={}", e.getMessage(), e);
            msg.setMsg("Failed to save report: " + e.getMessage());
            api.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            api.setMessage(msg.getMsg());
            api.setResponse(msg);
        }

        return api;
    }

    private Date parseDateOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Date.valueOf(LocalDate.parse(value));
        } catch (Exception e) {
            log.warn("Could not parse date value: {}", value);
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}