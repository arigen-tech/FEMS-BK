package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.DocApprovalStatus;
import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.repository.DocumentDetailsRepository;
import com.dmsBackend.service.DocumentsAuditLogService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service

public class DocHeaderStatusService {

    private final DocumentDetailsRepository detailsRepo;
    private final DocumentsAuditLogService auditService;

    public DocHeaderStatusService(DocumentDetailsRepository detailsRepo,
                                  DocumentsAuditLogService auditService) {
        this.detailsRepo = detailsRepo;
        this.auditService = auditService;
    }

    /**
     * Recalculate and update header.approvalStatus based on current detail statuses.
     */
    @Transactional
    public void recalcAndUpdateHeaderStatus(DocumentHeader header, String performedBy) {
        Integer headerId = header.getId();

        long total = detailsRepo.countByDocumentHeaderId(headerId);

        if (total == 0) {
            // No details → reset everything
            header.setApprovalStatus(DocApprovalStatus.PENDING);
            header.setIfPending(true);
            header.setIfApproved(false);
            header.setIfRejected(false);
            return;
        }

        long pending  = detailsRepo.countByDocumentHeaderIdAndStatus(headerId, DocApprovalStatus.PENDING);
        long approved = detailsRepo.countByDocumentHeaderIdAndStatus(headerId, DocApprovalStatus.APPROVED);
        long rejected = detailsRepo.countByDocumentHeaderIdAndStatus(headerId, DocApprovalStatus.REJECTED);

    /* -------------------------------
       1️⃣ Update FLAGS (your requirement)
       ------------------------------- */
        header.setIfPending(pending > 0);
        header.setIfApproved(approved > 0);
        header.setIfRejected(rejected > 0);

    /* -------------------------------
       2️⃣ Decide HEADER STATUS
       ------------------------------- */
        DocApprovalStatus newStatus;

        if (pending == total) {
            newStatus = DocApprovalStatus.PENDING;
        } else if (approved == total) {
            newStatus = DocApprovalStatus.APPROVED;
        } else if (rejected == total) {
            newStatus = DocApprovalStatus.REJECTED;
        } else if (pending > 0) {
            newStatus = DocApprovalStatus.PARTIALLY_PENDING;
        } else {
            // Approved + Rejected only
            if (approved >= rejected) {
                newStatus = DocApprovalStatus.PARTIALLY_APPROVED;
            } else {
                newStatus = DocApprovalStatus.PARTIALLY_REJECT;
            }
        }

        updateHeaderStatusIfChanged(header, newStatus, performedBy);
    }

    private void updateHeaderStatusIfChanged(DocumentHeader header,
                                             DocApprovalStatus newStatus,
                                             String by) {

        if (header.getApprovalStatus() != newStatus) {
            header.setApprovalStatus(newStatus);
            header.setUpdatedBy(by);
            header.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
        }
    }

}
