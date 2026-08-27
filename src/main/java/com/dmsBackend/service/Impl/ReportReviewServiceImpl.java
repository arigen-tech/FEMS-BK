package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.*;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.repository.*;
import com.dmsBackend.response.*;
import com.dmsBackend.service.ReportReviewService;
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
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportReviewServiceImpl implements ReportReviewService {

    @Autowired private ReportEntryRepository reportEntryRepository;
    @Autowired private DocumentHeaderRepository documentHeaderRepository;
    @Autowired private DepartmentMasterRepository departmentMasterRepository;
    @Autowired private StateMasterRepository stateMasterRepository;
    @Autowired private DistrictMasterRepository districtMasterRepository;
    @Autowired private CityMasterRepository cityMasterRepository;
    @Autowired private DocumentDetailsRepository documentDetailsRepository;
    @Autowired private BranchMasterRepository branchMasterRepository;

    @Autowired private CurrentUser currentUser;

    @Value("${report.storage.path:${document.storage.path}/reports}")
    private String reportStoragePath;

    @Override
    @Transactional(readOnly = true)
    public List<ReportReviewListItem> getPendingReviews() {
        Employee me = currentUser.getCurrentEmployeeOrThrow();

        log.info("API CALL → Get Pending Reviews | reviewerId={}", me.getId());

        List<ReportEntry> pendingReports = reportEntryRepository.findPendingReviewReports();

        if (pendingReports.isEmpty()) {
            log.info("No reports with pending status, trying to get all submitted reports");
            pendingReports = reportEntryRepository.findAllSubmittedReports();
            log.info("Found {} total submitted reports", pendingReports.size());
        }

        List<ReportReviewListItem> result = new ArrayList<>();

        for (ReportEntry report : pendingReports) {
            String reviewStatus = report.getReviewStatus();

            // Only reports still awaiting action belong in this queue.
            // APPROVED reports move to Dispatch; REJECTED reports go back to the
            // officer as a DRAFT and reappear here once resubmitted as PENDING.
            boolean stillPending = reviewStatus == null
                    || reviewStatus.isEmpty()
                    || reviewStatus.equals("PENDING")
                    || reviewStatus.equals("REFERRED");

            if (!stillPending) {
                log.info("Skipping report {} with reviewStatus={}", report.getId(), reviewStatus);
                continue;
            }

            ReportReviewListItem item = new ReportReviewListItem();
            item.setReportEntryId(report.getId());

            if (report.getDocumentHeader() != null) {
                item.setDocumentHeaderId(report.getDocumentHeader().getId());
                item.setFileNo(report.getDocumentHeader().getFileNo());
                item.setCaseId(report.getDocumentHeader().getCaseId());
                item.setFirNumber(report.getDocumentHeader().getFirNumber());
                item.setPoliceStation(report.getDocumentHeader().getPoliceStation());
                item.setCaseTitle(report.getDocumentHeader().getTitle());
            }

            if (report.getEmployee() != null) {
                item.setScientificOfficerName(report.getEmployee().getName());
                item.setScientificOfficerId(report.getEmployee().getId());
            }

            if (report.getDocumentHeader() != null && report.getDocumentHeader().getCaseType() != null) {
                item.setCaseType(report.getDocumentHeader().getCaseType().getName());
            }

            item.setReportTitle(report.getReportTitle());
            item.setSubmittedDate(report.getSubmittedOn());
            item.setReviewStatus(reviewStatus != null ? reviewStatus : "PENDING");

            if (report.getDocumentHeader() != null && report.getDocumentHeader().getDocumentDetails() != null) {
                item.setEvidenceCount(report.getDocumentHeader().getDocumentDetails().size());
            }

            result.add(item);
        }

        log.info("SUCCESS → Found {} pending reviews", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportReviewListItem> getReviewedReports(String reviewStatus) {
        Employee me = currentUser.getCurrentEmployeeOrThrow();

        log.info("API CALL → Get Reviewed Reports | reviewerId={} status={}", me.getId(), reviewStatus);

        List<ReportEntry> reviewedReports = reportEntryRepository.findByReviewStatus(reviewStatus);

        List<ReportReviewListItem> result = new ArrayList<>();

        for (ReportEntry report : reviewedReports) {
            ReportReviewListItem item = new ReportReviewListItem();
            item.setReportEntryId(report.getId());

            if (report.getDocumentHeader() != null) {
                item.setDocumentHeaderId(report.getDocumentHeader().getId());
                item.setFileNo(report.getDocumentHeader().getFileNo());
                item.setCaseId(report.getDocumentHeader().getCaseId());
                item.setFirNumber(report.getDocumentHeader().getFirNumber());
                item.setPoliceStation(report.getDocumentHeader().getPoliceStation());
                item.setCaseTitle(report.getDocumentHeader().getTitle());
            }

            if (report.getEmployee() != null) {
                item.setScientificOfficerName(report.getEmployee().getName());
                item.setScientificOfficerId(report.getEmployee().getId());
            }

            if (report.getDocumentHeader() != null && report.getDocumentHeader().getCaseType() != null) {
                item.setCaseType(report.getDocumentHeader().getCaseType().getName());
            }

            item.setReportTitle(report.getReportTitle());
            item.setSubmittedDate(report.getSubmittedOn());
            item.setReviewStatus(report.getReviewStatus());
            item.setReviewedOn(report.getReviewedOn());

            result.add(item);
        }

        log.info("SUCCESS → Found {} reports with status {}", result.size(), reviewStatus);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportReviewDetailResponse getReportForReview(Integer reportEntryId) {
        Employee me = currentUser.getCurrentEmployeeOrThrow();

        log.info("API CALL → Get Report For Review | reportEntryId={} reviewerId={}",
                reportEntryId, me.getId());

        ReportEntry report = reportEntryRepository.findById(reportEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportEntryId));

        ReportReviewDetailResponse resp = new ReportReviewDetailResponse();

        resp.setReportEntryId(report.getId());
        resp.setReportTitle(report.getReportTitle());
        resp.setReportSummary(report.getReportSummary());
        resp.setObservations(report.getObservations());
        resp.setScientificOpinion(report.getScientificOpinion());
        resp.setExaminationRemarks(report.getExaminationRemarks());
        resp.setExaminationStartDate(report.getExaminationStartDate());
        resp.setExaminationEndDate(report.getExaminationEndDate());
        resp.setStatus(report.getStatus().name());
        resp.setReviewStatus(report.getReviewStatus());
        resp.setSubmittedOn(report.getSubmittedOn());
        resp.setScientificReportPath(report.getScientificReportPath());
        resp.setFinalReportPath(report.getFinalReportPath());

        if (report.getExaminationMethod() != null) {
            resp.setExaminationMethodName(report.getExaminationMethod().getName());
        }

        if (report.getDocumentHeader() != null) {
            DocumentHeader header = report.getDocumentHeader();
            resp.setDocumentHeaderId(header.getId());
            resp.setCaseId(header.getCaseId());
            resp.setFileNo(header.getFileNo());
            resp.setFirNumber(header.getFirNumber());
            resp.setPoliceStation(header.getPoliceStation());
            resp.setCaseTitle(header.getTitle());
            resp.setSubject(header.getSubject());

            if (header.getCaseType() != null) {
                resp.setCaseType(header.getCaseType().getName());
            }
        }

        if (report.getEmployee() != null) {
            resp.setScientificOfficerName(report.getEmployee().getName());
            resp.setScientificOfficerId(report.getEmployee().getId());
        }

        if (report.getAttachments() != null) {
            List<ReportReviewDetailResponse.AttachmentRow> attachments = report.getAttachments()
                    .stream()
                    .map(a -> {
                        ReportReviewDetailResponse.AttachmentRow row = new ReportReviewDetailResponse.AttachmentRow();
                        row.setId(a.getId());
                        row.setFileName(a.getFileName());
                        row.setFilePath(a.getFilePath());
                        return row;
                    })
                    .collect(Collectors.toList());
            resp.setAttachments(attachments);
        }

        log.info("SUCCESS → Got report for review | reportEntryId={}", reportEntryId);
        return resp;
    }

    @Transactional
    @Override
    public ApiResponse<MessageResponse> reviewReport(
            Integer reportEntryId,
            Integer documentDetailId,
            String action,
            String comments,
            MultipartFile finalReport) {

        log.info("API CALL → Review Report | reportEntryId={} documentDetailId={} action={}",
                reportEntryId, documentDetailId, action);

        MessageResponse msg = new MessageResponse();
        ApiResponse<MessageResponse> api = new ApiResponse<>();
        Employee me = currentUser.getCurrentEmployeeOrThrow();

        try {
            ReportEntry report = reportEntryRepository.findById(reportEntryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportEntryId));

            if (!"APPROVE".equalsIgnoreCase(action) && !"REJECT".equalsIgnoreCase(action)) {
                msg.setMsg("Invalid action. Must be APPROVE or REJECT");
                api.setStatus(HttpStatus.BAD_REQUEST.value());
                api.setMessage(msg.getMsg());
                api.setResponse(msg);
                return api;
            }

            String newStatus = "APPROVE".equalsIgnoreCase(action) ? "APPROVED" : "REJECTED";
            Timestamp now = new Timestamp(System.currentTimeMillis());

            report.setReviewStatus(newStatus);
            if ("APPROVE".equalsIgnoreCase(action)) {
                report.setDispatchStatus("PENDING");
            }
            report.setReviewedBy(me.getId());
            report.setReviewedOn(now);
            report.setReviewComments(comments);

            // ── On REJECT/Return: send the report back to the officer as an editable draft ──
            if ("REJECT".equalsIgnoreCase(action)) {
                report.setStatus(ReportEntry.ReportStatus.DRAFT);
                report.setSubmittedOn(null); // clears "submitted" state so it reappears as pending work
            }

            String savedFinalReportPath = null;
            if (finalReport != null && !finalReport.isEmpty()) {
                Path reportDir = Paths.get(reportStoragePath, "reviewed_reports", "report_" + reportEntryId);
                Files.createDirectories(reportDir);

                String safeName = System.currentTimeMillis() + "_" +
                        finalReport.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                Path dest = reportDir.resolve(safeName);

                try (InputStream in = finalReport.getInputStream()) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }

                savedFinalReportPath = dest.toString();
                report.setFinalReportPath(savedFinalReportPath);
            }

            reportEntryRepository.save(report);

            if (documentDetailId != null) {
                DocumentDetails doc = documentDetailsRepository.findById(documentDetailId).orElse(null);
                if (doc != null) {
                    doc.setReviewStatus(newStatus);
                    doc.setReviewedBy(me.getId());
                    doc.setReviewedOn(now);
                    doc.setReviewComments(comments);
                    if (savedFinalReportPath != null) {
                        doc.setFinalReportPath(savedFinalReportPath);
                    }
                    documentDetailsRepository.save(doc);
                } else {
                    log.warn("documentDetailId={} not found while reviewing reportEntryId={}",
                            documentDetailId, reportEntryId);
                }
            }

            msg.setMsg("Report " + (action.equalsIgnoreCase("APPROVE") ? "approved" : "returned for revision") + " successfully");
            api.setStatus(HttpStatus.OK.value());
            api.setMessage("Success");
            api.setResponse(msg);

            log.info("SUCCESS → Report {} | reportEntryId={} documentDetailId={} reviewerId={}",
                    action.toUpperCase(), reportEntryId, documentDetailId, me.getId());

        } catch (IOException e) {
            log.error("FAILED → Review Report (file IO) | reason={}", e.getMessage(), e);
            msg.setMsg("Failed to save final report: " + e.getMessage());
            api.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            api.setMessage(msg.getMsg());
            api.setResponse(msg);
        } catch (Exception e) {
            log.error("FAILED → Review Report | reason={}", e.getMessage(), e);
            msg.setMsg("Failed to review report: " + e.getMessage());
            api.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            api.setMessage(msg.getMsg());
            api.setResponse(msg);
        }

        return api;
    }

    @Transactional
    @Override
    public ApiResponse<MessageResponse> referReport(
            Integer reportEntryId,
            Integer documentDetailId,
            String toLaboratory,
            String fromLaboratory,
            String reason,
            MultipartFile supportingDocument) {

        log.info("API CALL → Refer Report | reportEntryId={} documentDetailId={} fromLab={} toLab={}",
                reportEntryId, documentDetailId, fromLaboratory, toLaboratory);

        MessageResponse msg = new MessageResponse();
        ApiResponse<MessageResponse> api = new ApiResponse<>();
        Employee me = currentUser.getCurrentEmployeeOrThrow();

        try {
            ReportEntry report = reportEntryRepository.findById(reportEntryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportEntryId));

            Timestamp now = new Timestamp(System.currentTimeMillis());

            report.setReviewStatus("REFERRED");
            report.setReferralStatus("REFERRED");
            report.setReferredToLab(toLaboratory);
            report.setReferredFromLab(fromLaboratory);
            report.setReferredOn(now);
            report.setReferralReason(reason);
            report.setReviewedBy(me.getId());
            report.setReviewedOn(now);

            String savedDocPath = null;
            if (supportingDocument != null && !supportingDocument.isEmpty()) {
                Path reportDir = Paths.get(reportStoragePath, "referred_reports", "report_" + reportEntryId);
                Files.createDirectories(reportDir);

                String safeName = System.currentTimeMillis() + "_" +
                        supportingDocument.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                Path dest = reportDir.resolve(safeName);

                try (InputStream in = supportingDocument.getInputStream()) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }

                savedDocPath = dest.toString();
                report.setFinalReportPath(savedDocPath);
            }

            reportEntryRepository.save(report);

            if (documentDetailId != null) {
                DocumentDetails doc = documentDetailsRepository.findById(documentDetailId).orElse(null);
                if (doc != null) {
                    doc.setReviewStatus("REFERRED");
                    doc.setReferralStatus("REFERRED");
                    doc.setReferredToLab(toLaboratory);
                    doc.setReferredFromLab(fromLaboratory);
                    doc.setReferredOn(now);
                    doc.setReferralReason(reason);
                    doc.setReviewedBy(me.getId());
                    doc.setReviewedOn(now);
                    if (savedDocPath != null) {
                        doc.setFinalReportPath(savedDocPath);
                    }
                    documentDetailsRepository.save(doc);
                } else {
                    log.warn("documentDetailId={} not found while referring reportEntryId={}",
                            documentDetailId, reportEntryId);
                }
            }

            msg.setMsg("Report referred/transferred successfully");
            api.setStatus(HttpStatus.OK.value());
            api.setMessage("Success");
            api.setResponse(msg);

            log.info("SUCCESS → Report Referred | reportEntryId={} documentDetailId={} toLab={} by={}",
                    reportEntryId, documentDetailId, toLaboratory, me.getId());

        } catch (IOException e) {
            log.error("FAILED → Refer Report (file IO) | reason={}", e.getMessage(), e);
            msg.setMsg("Failed to save referral document: " + e.getMessage());
            api.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            api.setMessage(msg.getMsg());
            api.setResponse(msg);
        } catch (Exception e) {
            log.error("FAILED → Refer Report | reason={}", e.getMessage(), e);
            msg.setMsg("Failed to refer report: " + e.getMessage());
            api.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            api.setMessage(msg.getMsg());
            api.setResponse(msg);
        }

        return api;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceReviewListItem> getEvidenceForCase(Integer documentHeaderId) {
        Employee me = currentUser.getCurrentEmployeeOrThrow();

        log.info("API CALL → Get Evidence For Case | documentHeaderId={} reviewerId={}",
                documentHeaderId, me.getId());

        List<DocumentDetails> evidenceList =
                documentDetailsRepository.findByDocumentHeader_IdAndIsDeletedFalse(documentHeaderId)
                        .stream()
                        .filter(d -> d.getReferralStatus() == null || d.getReferralStatus().isBlank())
                        .collect(Collectors.toList());

        List<ReportEntry> caseReports = reportEntryRepository.findByDocumentHeader_Id(documentHeaderId);

        List<EvidenceReviewListItem> result = new ArrayList<>();

        for (DocumentDetails doc : evidenceList) {
            EvidenceReviewListItem item = new EvidenceReviewListItem();
            item.setDocumentDetailId(doc.getId());
            item.setEvidenceId(doc.getDocumentHeader().getEvidenceId());

            if (doc.getEvidenceTypeId() != null) {
                item.setEvidenceCategory(doc.getDocumentHeader().getCategoryMaster().getName());
                item.setEvidenceType(doc.getEvidenceTypeId().getName());
            }

            if (doc.getDepartment() != null) {
                item.setDivision(doc.getDepartment().getName());
            }

            if (doc.getEmpId() != null) {
                item.setScientificOfficerName(doc.getEmpId().getName());
                item.setScientificOfficerId(doc.getEmpId().getId());
            }

            ReportEntry matchedReport = caseReports.stream()
                    .filter(r -> doc.getEmpId() != null
                            && r.getEmployee() != null
                            && r.getEmployee().getId().equals(doc.getEmpId().getId()))
                    .findFirst()
                    .orElse(null);

            if (matchedReport != null) {
                item.setReportEntryId(matchedReport.getId());
            }

            item.setReportStatus(doc.getReviewStatus() != null ? doc.getReviewStatus() : "PENDING");
            item.setReferralStatus(doc.getReferralStatus() != null ? doc.getReferralStatus() : "--");

            result.add(item);
        }

        log.info("SUCCESS → Found {} evidence items for documentHeaderId={}", result.size(), documentHeaderId);
        return result;
    }

    private String resolveBranchName(String branchId) {
        if (branchId == null || branchId.isBlank()) return "--";
        try {
            return branchMasterRepository.findNameById(Integer.parseInt(branchId))
                    .orElse(branchId);
        } catch (NumberFormatException e) {
            return branchId;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralListItem> getOutgoingReferrals() {
        Employee me = currentUser.getCurrentEmployeeOrThrow();
        String myBranchId = me.getBranch() != null ? String.valueOf(me.getBranch().getId()) : null;

        log.info("API CALL → Get Outgoing Referrals | branchId={} userId={}", myBranchId, me.getId());

        if (myBranchId == null) return new ArrayList<>();

        List<DocumentDetails> referred = documentDetailsRepository.findByReferredFromLab(myBranchId);

        List<ReferralListItem> result = new ArrayList<>();
        for (DocumentDetails doc : referred) {
            ReferralListItem item = new ReferralListItem();
            item.setDocumentDetailId(doc.getId());
            item.setCaseNo(doc.getDocumentHeader() != null ? doc.getDocumentHeader().getFileNo() : "--");
            item.setEvidenceId(doc.getDocumentHeader() != null ? doc.getDocumentHeader().getEvidenceId() : "--");
            item.setEvidenceType(doc.getEvidenceTypeId() != null ? doc.getEvidenceTypeId().getName() : "--");
            item.setFromLaboratoryName(resolveBranchName(doc.getReferredFromLab()));
            item.setToLaboratoryName(resolveBranchName(doc.getReferredToLab()));
            item.setReferredOn(doc.getReferredOn());
            item.setReferralStatus(doc.getReferralStatus());
            item.setReferralReason(doc.getReferralReason());

            // ── Resolve reportEntryId so the frontend can pull full report content ──
            if (doc.getDocumentHeader() != null && doc.getEmpId() != null) {
                reportEntryRepository.findByDocumentHeader_Id(doc.getDocumentHeader().getId())
                        .stream()
                        .filter(r -> r.getEmployee() != null && r.getEmployee().getId().equals(doc.getEmpId().getId()))
                        .findFirst()
                        .ifPresent(r -> item.setReportEntryId(r.getId()));
            }

            result.add(item);
        }

        log.info("SUCCESS → Found {} outgoing referrals", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralListItem> getIncomingReferrals() {
        Employee me = currentUser.getCurrentEmployeeOrThrow();
        String myBranchId = me.getBranch() != null ? String.valueOf(me.getBranch().getId()) : null;

        log.info("API CALL → Get Incoming Referrals | branchId={} userId={}", myBranchId, me.getId());

        if (myBranchId == null) return new ArrayList<>();

        List<DocumentDetails> pending =
                documentDetailsRepository.findByReferredToLabAndReferralStatus(myBranchId, "REFERRED");

        List<ReferralListItem> result = new ArrayList<>();
        for (DocumentDetails doc : pending) {
            ReferralListItem item = new ReferralListItem();
            item.setDocumentDetailId(doc.getId());
            item.setCaseNo(doc.getDocumentHeader() != null ? doc.getDocumentHeader().getFileNo() : "--");
            item.setEvidenceId(doc.getDocumentHeader() != null ? doc.getDocumentHeader().getEvidenceId() : "--");
            item.setEvidenceType(doc.getEvidenceTypeId() != null ? doc.getEvidenceTypeId().getName() : "--");
            item.setFromLaboratoryName(resolveBranchName(doc.getReferredFromLab()));
            item.setToLaboratoryName(resolveBranchName(doc.getReferredToLab()));
            item.setReferredOn(doc.getReferredOn());
            item.setReferralStatus(doc.getReferralStatus());
            item.setReferralReason(doc.getReferralReason());

            // ── Resolve reportEntryId so the frontend can pull full report content ──
            if (doc.getDocumentHeader() != null && doc.getEmpId() != null) {
                reportEntryRepository.findByDocumentHeader_Id(doc.getDocumentHeader().getId())
                        .stream()
                        .filter(r -> r.getEmployee() != null && r.getEmployee().getId().equals(doc.getEmpId().getId()))
                        .findFirst()
                        .ifPresent(r -> item.setReportEntryId(r.getId()));
            }

            result.add(item);
        }

        log.info("SUCCESS → Found {} incoming referrals", result.size());
        return result;
    }

    @Transactional
    @Override
    public ApiResponse<MessageResponse> acceptReferral(Integer documentDetailId) {
        log.info("API CALL → Accept Referral | documentDetailId={}", documentDetailId);

        MessageResponse msg = new MessageResponse();
        ApiResponse<MessageResponse> api = new ApiResponse<>();
        Employee me = currentUser.getCurrentEmployeeOrThrow();

        try {
            DocumentDetails doc = documentDetailsRepository.findById(documentDetailId)
                    .orElseThrow(() -> new ResourceNotFoundException("Evidence not found: " + documentDetailId));

            if (!"REFERRED".equals(doc.getReferralStatus())) {
                msg.setMsg("This item is not pending acceptance.");
                api.setStatus(HttpStatus.BAD_REQUEST.value());
                api.setMessage(msg.getMsg());
                api.setResponse(msg);
                return api;
            }

            doc.setDepartment(null);
            doc.setEmpId(null);
            doc.setReferralStatus("ACCEPTED");
            doc.setReferralAcceptedBy(me.getId());
            doc.setReferralAcceptedOn(new Timestamp(System.currentTimeMillis()));

            documentDetailsRepository.save(doc);

            msg.setMsg("Referral accepted successfully");
            api.setStatus(HttpStatus.OK.value());
            api.setMessage("Success");
            api.setResponse(msg);

            log.info("SUCCESS → Referral Accepted | documentDetailId={} by={}", documentDetailId, me.getId());

        } catch (ResourceNotFoundException e) {
            msg.setMsg(e.getMessage());
            api.setStatus(HttpStatus.NOT_FOUND.value());
            api.setMessage(msg.getMsg());
            api.setResponse(msg);
        } catch (Exception e) {
            log.error("FAILED → Accept Referral | reason={}", e.getMessage(), e);
            msg.setMsg("Failed to accept referral: " + e.getMessage());
            api.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            api.setMessage(msg.getMsg());
            api.setResponse(msg);
        }

        return api;
    }
}