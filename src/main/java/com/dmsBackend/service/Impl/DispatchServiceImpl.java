package com.dmsBackend.service.Impl;


import com.dmsBackend.entity.*;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.repository.*;
import com.dmsBackend.response.*;
import com.dmsBackend.service.DispatchService;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DispatchServiceImpl implements DispatchService {

    @Autowired private ReportEntryRepository reportEntryRepository;
    @Autowired private DocumentForwardingAuthorityRepository documentForwardingAuthorityRepository;
    @Autowired private CurrentUser currentUser;

    @Value("${report.storage.path:${document.storage.path}/reports}")
    private String reportStoragePath;

    @Override
    @Transactional(readOnly = true)
    public List<DispatchListItem> getPendingDispatchList() {
        log.info("API CALL → Get Pending Dispatch List");

        List<ReportEntry> approvedReports = reportEntryRepository.findByReviewStatus("APPROVED");

        List<DispatchListItem> result = new ArrayList<>();

        for (ReportEntry report : approvedReports) {
            // Skip already dispatched reports
            if ("DISPATCHED".equals(report.getDispatchStatus())) {
                continue;
            }

            DispatchListItem item = new DispatchListItem();
            item.setReportEntryId(report.getId());
            item.setReportNumber("RPT-" + String.format("%03d", report.getId()));
            item.setApprovedDate(report.getReviewedOn());
            item.setDispatchStatus(report.getDispatchStatus() != null ? report.getDispatchStatus() : "PENDING");

            if (report.getDocumentHeader() != null) {
                item.setCaseNumber(report.getDocumentHeader().getFileNo());
                item.setFirNumber(report.getDocumentHeader().getFirNumber());
            }

            if (report.getEmployee() != null && report.getEmployee().getDepartment() != null) {
                item.setDivisionName(report.getEmployee().getDepartment().getName());
            }

            result.add(item);
        }

        log.info("SUCCESS → Found {} pending dispatches", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public DispatchDetailResponse getDispatchDetail(Integer reportEntryId) {
        log.info("API CALL → Get Dispatch Detail | reportEntryId={}", reportEntryId);

        ReportEntry report = reportEntryRepository.findById(reportEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportEntryId));

        DispatchDetailResponse resp = new DispatchDetailResponse();
        resp.setReportEntryId(report.getId());

        if (report.getDocumentHeader() != null) {
            resp.setCaseNumber(report.getDocumentHeader().getFileNo());
            resp.setFirNumber(report.getDocumentHeader().getFirNumber());

            documentForwardingAuthorityRepository.findByDocumentHeader_Id(report.getDocumentHeader().getId())
                    .ifPresent(authority -> {
                        resp.setForwardingAuthorityName(authority.getAuthorityName());
                        resp.setForwardingDesignation(authority.getDesignation());
                        resp.setForwardingOrganisation(authority.getOrganisation());
                        resp.setForwardingLetterNumber(authority.getForwardingLetterNumber());
                    });
        }

        resp.setDispatchDate(report.getDispatchDate());
        resp.setDispatchReferenceNo(report.getDispatchReferenceNo());
        resp.setRecipient(report.getRecipient());
        resp.setDispatchMode(report.getDispatchMode());
        resp.setDispatchDocumentPath(report.getDispatchDocumentPath());
        resp.setDispatchRemarks(report.getDispatchRemarks());
        resp.setNotifyEmail(report.getNotifyEmail());
        resp.setNotifySms(report.getNotifySms());
        resp.setDispatchStatus(report.getDispatchStatus() != null ? report.getDispatchStatus() : "PENDING");

        log.info("SUCCESS → Got dispatch detail | reportEntryId={}", reportEntryId);
        return resp;
    }

    @Transactional
    @Override
    public ApiResponse<MessageResponse> saveDispatch(
            Integer reportEntryId,
            String dispatchDate,
            String dispatchReferenceNo,
            String recipient,
            String dispatchMode,
            String dispatchRemarks,
            Boolean notifyEmail,
            Boolean notifySms,
            MultipartFile dispatchDocument) {

        log.info("API CALL → Save Dispatch | reportEntryId={}", reportEntryId);

        MessageResponse msg = new MessageResponse();
        ApiResponse<MessageResponse> api = new ApiResponse<>();
        Employee me = currentUser.getCurrentEmployeeOrThrow();

        try {
            ReportEntry report = reportEntryRepository.findById(reportEntryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportEntryId));

            if (dispatchDate != null && !dispatchDate.isBlank()) {
                report.setDispatchDate(Date.valueOf(LocalDate.parse(dispatchDate)));
            }
            report.setDispatchReferenceNo(dispatchReferenceNo);
            report.setRecipient(recipient);
            report.setDispatchMode(dispatchMode);
            report.setDispatchRemarks(dispatchRemarks);
            report.setNotifyEmail(Boolean.TRUE.equals(notifyEmail));
            report.setNotifySms(Boolean.TRUE.equals(notifySms));

            if (dispatchDocument != null && !dispatchDocument.isEmpty()) {
                Path dispatchDir = Paths.get(reportStoragePath, "dispatch_documents", "report_" + reportEntryId);
                Files.createDirectories(dispatchDir);

                String safeName = System.currentTimeMillis() + "_" +
                        dispatchDocument.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                Path dest = dispatchDir.resolve(safeName);

                try (InputStream in = dispatchDocument.getInputStream()) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }

                report.setDispatchDocumentPath(dest.toString());
            }

            report.setDispatchStatus("DISPATCHED");
            report.setDispatchedBy(me.getId());
            report.setDispatchedOn(new Timestamp(System.currentTimeMillis()));

            reportEntryRepository.save(report);

            msg.setMsg("Dispatch saved successfully");
            api.setStatus(HttpStatus.OK.value());
            api.setMessage("Success");
            api.setResponse(msg);

            log.info("SUCCESS → Dispatch Saved | reportEntryId={} by={}", reportEntryId, me.getId());

        } catch (IOException e) {
            log.error("FAILED → Save Dispatch (file IO) | reason={}", e.getMessage(), e);
            msg.setMsg("Failed to save dispatch document: " + e.getMessage());
            api.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            api.setMessage(msg.getMsg());
            api.setResponse(msg);
        } catch (Exception e) {
            log.error("FAILED → Save Dispatch | reason={}", e.getMessage(), e);
            msg.setMsg("Failed to save dispatch: " + e.getMessage());
            api.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            api.setMessage(msg.getMsg());
            api.setResponse(msg);
        }

        return api;
    }
}
