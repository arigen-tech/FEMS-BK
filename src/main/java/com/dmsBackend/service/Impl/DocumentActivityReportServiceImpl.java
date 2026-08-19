package com.dmsBackend.service.Impl;

import com.dmsBackend.constants.DocumentActivityReportSpecification;
import com.dmsBackend.entity.*;
import com.dmsBackend.repository.DocumentActivityReportRopository;
import com.dmsBackend.response.DocumentActivityReportRequest;
import com.dmsBackend.response.DocumentActivityReportResponse;
import com.dmsBackend.service.DocumentActivityReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentActivityReportServiceImpl
        implements DocumentActivityReportService {

    @Autowired
    private DocumentActivityReportRopository repository;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public void logAction(
            DocumentHeader header,
            DocumentDetails detail,
            ActionTypeForReport actionType,
            String status,
            Employee actor,
            HttpServletRequest request,
            Map<String, Object> extra) {

        header = entityManager.merge(header);

        DocumentActivityReport r = new DocumentActivityReport();

        // -------- Header data --------
        r.setDocumentHdId(Long.valueOf(header.getId()));
        r.setDocumentName(header.getFileNo());
        r.setTitle(header.getTitle());
        r.setSubject(header.getSubject());
        r.setDepartmentId(Long.valueOf(header.getDepartmentMaster().getId()));
        r.setBranchId(Long.valueOf(header.getBranchMaster().getId()));
        if (header.getCategoryMaster() != null) {
            r.setCategory(header.getCategoryMaster().getName());
            r.setCategoryId(Long.valueOf(header.getCategoryMaster().getId()));
        }

        // -------- Detail data --------
        if (detail != null) {
            r.setDocumentDtId(Long.valueOf(detail.getId()));
            r.setFileName(detail.getDocName());
            r.setVersion(detail.getVersion());
            r.setFileSize(detail.getFileSizeHuman());
        }

        // -------- Action --------
        r.setActionType(actionType);
        r.setActionDate(LocalDateTime.now());
        r.setActionBy(actor.getEmail());
        r.setEmpId(Long.valueOf(actor.getId()));
        r.setUserRole(actor.getRole() != null ? actor.getRole().getRole() : null);
        r.setStatus(status);

        // -------- Tracking --------
        if (request != null) {
            r.setIpAddress(request.getRemoteAddr());
        } else {
            r.setIpAddress("SYSTEM");
        }


        if (extra != null) {
            r.setRemarks(extra.toString()); // simple JSON-style storage
            if (extra.containsKey("jobId")) {
                r.setJobId(String.valueOf(extra.get("jobId")));
            }
            if (extra.containsKey("location")) {
                r.setLocation(String.valueOf(extra.get("location")));
            }
            if (extra.containsKey("retentionUntil")) {
                r.setRetentionUntil((LocalDate) extra.get("retentionUntil"));
            }
        }

        // -------- Audit timestamp (was missing → caused CREATED_AT NOT NULL violation) --------
        r.setCreatedAt(LocalDateTime.now());

        repository.save(r);
    }

    @Override
    public List<DocumentActivityReportResponse> search(DocumentActivityReportRequest request) {
        if (request.getActionTypes() == null || request.getActionTypes().isEmpty()) {
            throw new IllegalArgumentException("ActionType is required");
        }

        List<DocumentActivityReport> reports =
                repository.findAll(DocumentActivityReportSpecification.search(request));

        return reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private DocumentActivityReportResponse mapToResponse(DocumentActivityReport r) {
        return DocumentActivityReportResponse.builder()
                .reportId(r.getReportId())
                .documentHdId(r.getDocumentHdId())
                .documentDtId(r.getDocumentDtId())
                .departmentId(r.getDepartmentId())
                .branchId(r.getBranchId())
                .categoryId(r.getCategoryId())
                .empId(r.getEmpId())
                .documentName(r.getDocumentName())
                .title(r.getTitle())
                .subject(r.getSubject())
                .category(r.getCategory())
                .fileName(r.getFileName())
                .version(r.getVersion())
                .fileSize(r.getFileSize())
                .actionType(r.getActionType())
                .actionDate(r.getActionDate())
                .actionBy(r.getActionBy())
                .userRole(r.getUserRole())
                .ipAddress(r.getIpAddress())
                .location(r.getLocation())
                .jobId(r.getJobId())
                .remarks(r.getRemarks())
                .status(r.getStatus())
                .retentionUntil(r.getRetentionUntil())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
